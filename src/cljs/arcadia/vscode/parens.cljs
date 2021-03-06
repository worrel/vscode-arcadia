(ns arcadia.vscode.parens
  (:require [clojure.string :as s]))

(def PARENS "[]{}()")

(defn add-to-form
  [acc c]
  (update acc :form
          (fn [curr c]
            (str (or curr "") c))
          c))

(defn kill-ws
  [text]
  (-> text
      (s/trim)
      ((fn [t] (when-not (empty? t) t)))))

(defn complete-form
  ([acc]
   (-> acc
       (:form)
       (kill-ws)
       ((fn [form]
          (if form
            (update acc :forms conj form)
            acc)))
       (dissoc :form)))
  ([acc c]
   (-> acc
       (update :form str c)
       (complete-form))))

(defn check-complete
  [{:keys [stack form opened] :as acc} ptype c]
  (cond
    (and
      (= :open ptype)
      (not (empty? form))
      (= opened 1))
    (complete-form acc)

    (and
      (= :close ptype)
      (empty? stack)
      (> opened 0))
    (complete-form acc c)

    (= :close ptype)
    (add-to-form acc c)

    :else acc))

(defn open-parens
  [acc p]
  (-> acc
      (update :stack conj p)
      (update :opened #(inc (or % 0)))
      (check-complete :open p)
      (add-to-form p)))

(defn close-parens
  [acc p]
  (-> acc
      (update :stack pop)
      (check-complete :close p)))

(defn parse-forms
  [text]
  (reduce
    (fn [{:keys [stack] :as acc} c]
      (let [pos (.indexOf PARENS c)]
        (cond
          (= -1 pos) (add-to-form acc c)

          (not= 0 (mod pos 2))
          (if (or (empty? stack)
                  (not= (.indexOf PARENS (peek stack)) (dec pos)))
            (reduced (add-to-form acc c))
            (close-parens acc c))

          :else (open-parens acc c))))
    {:stack []
     :forms []}
    text))

(defn check-forms
  [text]
  (-> text
      (parse-forms)
      ((fn [{:keys [stack forms form]}]
         (if (and (empty? stack)
                  (not (nil? form)))
           [(conj forms form) nil]
           [forms form])))))