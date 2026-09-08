(ns glassceramics.governor
  "GlassCeramicsPlantCoordGovernor — the independent safety/scope layer
  gating every plant scheduling/logistics proposal an advisor may make
  for a glass and ceramics plant-operator crew. The governor never
  dispatches hardware itself, never operates glass-melting furnace or
  ceramics kiln equipment itself, and never finalizes a
  plant-operation-execution decision (e.g. deciding to run or proceed
  with a specific glass-melting furnace or ceramics kiln cycle) or a
  plant-safety-clearance decision (e.g. declaring the plant floor
  safety-cleared), and never overrides a plant safety officer's
  judgment — those are permanently out of this actor's scope and
  remain a plant safety officer's exclusive judgment (README's
  'Robotics premise': this actor coordinates PLANT SCHEDULING/LOGISTICS
  ONLY — it never operates furnace/kiln equipment or makes
  safety-clearance decisions itself). Modeled closely on
  cloud-itonami-isco-8114's mineralplant.governor for the
  industrial-plant-safety-domain shape.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. operator provenance   — the crew member must be independently
                                verified/registered before any action.
    2. plant provenance      — the plant site must be independently
                                verified/registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never operates furnace/kiln equipment
                                itself; it only gates what the advisor
                                may coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                plant-operation-execution decision
                                (e.g. deciding to run or proceed with a
                                specific glass-melting furnace or
                                ceramics kiln cycle) or a
                                plant-safety-clearance decision (e.g.
                                declaring the plant floor
                                safety-cleared), or to override a
                                plant safety officer's judgment, is a
                                hard, permanent block — never
                                auto-commit-eligible, never merely an
                                escalation (checked both against the
                                proposed :op and, defense-in-depth,
                                against the proposal's :rationale text
                                — matched as full finalization/
                                execution ACTION phrases such as
                                \"proceed with the furnace operation\" /
                                \"finalize the furnace-operation
                                decision\" / \"declare the plant safety
                                cleared\" / \"override the plant safety
                                officer's judgment\", never as bare
                                nouns like \"glass\", \"ceramics\",
                                \"kiln\", \"furnace\" or \"safety\", so
                                the check can never self-trip on the
                                advisor's own routine rationale text,
                                e.g. \"logged work record for operator
                                …\" or \"scheduled crew operation for
                                production-run task …\" or \"…routed
                                for plant safety officer review\" — all
                                three legitimately contain those bare
                                nouns but none is a finalization
                                action, and all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a burn-hazard, furnace-condition or
                                equipment-condition concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [glassceramics.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-furnace-operation-decision :authorize-furnace-operation
    :proceed-with-furnace-operation :run-furnace-operation-cycle
    :finalize-kiln-operation-decision :authorize-kiln-operation
    :proceed-with-kiln-operation :run-kiln-operation-cycle
    :finalize-plant-safety-clearance :declare-plant-safety-cleared
    :override-plant-safety-officer-judgment
    :override-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("glass", "ceramics", "kiln", "furnace", "burn", "safety", "plant",
;; "officer") — so this can never match inside the mock advisor's own
;; default rationale text (which legitimately contains those bare
;; nouns, e.g. "production-run task" / "plant safety officer review").
;; See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the furnace operation" "proceed with furnace operation"
   "proceed with the kiln operation" "proceed with kiln operation"
   "authorize the furnace operation" "authorize furnace operation"
   "authorize the kiln operation" "authorize kiln operation"
   "finalize the furnace-operation decision" "finalize the furnace operation decision"
   "finalize the kiln-operation decision" "finalize the kiln operation decision"
   "declare the plant safety cleared" "declare plant safety cleared"
   "declare the plant floor safety cleared"
   "override the plant safety officer's judgment"
   "override the safety officer's judgment"
   "override plant safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal operator-record plant-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? operator-record)
      (conj {:rule :no-operator
             :detail "未登録 operator への提案は不可（operator record は独立して検証・登録済みでなければならない）"})

      (nil? plant-record)
      (conj {:rule :no-plant
             :detail "未登録 plant への提案は不可（plant record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は furnace/kiln 設備を直接操作しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "plant-operation 実行判断の確定・plant safety clearance 判断の確定・plant safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `glassceramics.store/Store`. Pure — never
  mutates the store, never dispatches a plant operation."
  [request _context proposal store]
  (let [operator-record (store/operator store (:operator-id request))
        plant-record (some->> (:plant-id proposal) (store/plant store))
        hard (hard-violations proposal operator-record plant-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
