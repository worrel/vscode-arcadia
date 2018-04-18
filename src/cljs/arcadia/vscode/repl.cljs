(ns arcadia.vscode.repl
  (:require [arcadia.vscode.parens :as p]
            [clojure.string :as string]
            [vscode.util 
              :as util 
              :refer [export! get-config new-promise new-Range resolved-promise then 
                      register-command! register-text-editor-command!]]))

(def dgram (js/require "dgram"))
(def vscode (js/require "vscode"))

(def repl (atom nil))

(def repl-options
  (let [conf (get-config "arcadia")]
    {:host (get-config conf "replHost")
     :port (get-config conf "replPort")}))

(defn handle-input
 [cmd]
 (new-promise
  (fn [resolve]
    (let [{:keys [input server output host port]} @repl]
      (swap! input str cmd)
      (let [[bs us] (p/check-forms @input)]
        (doseq [bal bs]
          (.appendLine output bal)
          (.send server bal port host))
        (reset! input (or us ""))
        (resolve true))))))

(defn parse-msg
  [msg]
  (-> msg 
      (.toString) 
      (.split "\n")
      ((juxt #(.pop %) #(.join % "\n")))))

(defn handle-response
 [output msg rinfo]
 (let [[prompt result] (parse-msg msg)]
  (.appendLine output result) 
  (.append output prompt)))

(def repl-init
 (quote 
    (binding [*warn-on-reflection* false]
        (do 
            (println "; Arcadia REPL")
            (println (str "; Clojure " (clojure-version)))
            (println (str "; Unity " (UnityEditorInternal.InternalEditorUtility/GetFullUnityVersion)))
            (println (str "; Mono " (.Invoke (.GetMethod Mono.Runtime "GetDisplayName" (enum-or System.Reflection.BindingFlags/NonPublic System.Reflection.BindingFlags/Static)) nil nil)))))))

(defn connect-repl 
 [output host port]
 (let [server (.createSocket dgram "udp4")]
  (.on server "error" #(println "Server error: " (js->clj %)))
  (.on server "message" (partial handle-response output))
  (.send server (str repl-init) port host)
  server))

(defn when-no-repl
 [f]
 (if @repl
  (resolved-promise true)
  (new-promise
    (fn [resolve]
      (resolve (f))))))

(defn start-repl* 
  []
  (println "Starting REPL...")
  (let [host (:host repl-options)
        port (:port repl-options)
        out (.createOutputChannel (.. vscode -window) "Arcadia REPL")
        conn (connect-repl out host port)]
    (.show out true)
    (println "REPL started!")
    (reset! repl
      {:server conn
        :input (atom "")
        :output out
        :host host
        :port port})))
            
(defn start-repl
  []
  (when-no-repl
    #(do (start-repl*) true))) ;; returning CLJ data structures to command handlers makes vscode lose its mind

(defn send 
  [msg]
  (-> (when-no-repl start-repl*)
      (then #(handle-input msg))
      (then #(let [{:keys [input output]} @repl]
                (when (not (empty? @input))
                  (.append output @input))))))

(defn send-line
  [editor]
  (send 
    (-> (.-document editor)
        (.lineAt (.. editor -selection -start))
        (.-text))))

(defn send-selection
  [editor]
  (send 
    (-> (.-document editor)
        (.getText (.-selection editor)))))
  
(defn send-file
  [editor]
  (send (.. editor -document getText)))

(defn is-comment
  [line]
  (let [firstChar (subs (string/trim (.-text line)) 0 1)]
  (= firstChar #";")))

(defn check-line
  [line]
  (not (or (.-isEmptyOrWhitespace line) (is-comment line))))

(defn send-region
  [editor]
  (let [activeLine (.. editor -selection -active -line)
        startLine (last (take-while (fn [n] (not (.. editor -document (lineAt n) -isEmptyOrWhitespace))) (take-while (partial < -1) (iterate dec activeLine))))
        endLines (filter (comp not is-comment) (take-while (fn [l] (not (.-isEmptyOrWhitespace l))) (map (fn [n] (.. editor -document (lineAt n))) (iterate inc (if (.. editor -document (lineAt startLine) -isEmptyOrWhitespace) (+ startLine 1) startLine)))))]
    (send (string/join "\n" (map aget endLines (repeat "text"))))))

(defn activate-repl
  []
  (println "Registering REPL commands")
  [ (register-command! "arcadia.replStart" start-repl)
    (register-text-editor-command! "arcadia.replSendLine" send-line)
    (register-text-editor-command! "arcadia.replSendSelection" send-selection)
    (register-text-editor-command! "arcadia.replSendRegion" send-region)
    (register-text-editor-command! "arcadia.replSendFile" send-file)])