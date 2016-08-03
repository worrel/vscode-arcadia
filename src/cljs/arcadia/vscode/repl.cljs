(ns arcadia.vscode.repl
  (:require [vscode.util 
              :as util 
              :refer [export! get-config new-promise resolved-promise then 
                      register-command! register-text-editor-command!]]))

(def dgram (js/require "dgram"))
(def vscode (js/require "vscode"))

(def repl-options {:host (get-config "arcadia.replHost") 
                   :port (get-config "arcadia.replPort")})

(def PARENS "[]{}()")

(def repl (atom nil))

(defn process-parens
 [form]
 (reduce 
   (fn [stack c]
    (let [pos (.indexOf PARENS c)]
      (cond
        (= -1 pos) stack
        (not= 0 (mod pos 2))
        (if (or (empty? stack)
                (not= (.indexOf PARENS (peek stack)) (dec pos)))
            (reduced :unbalanced)
            (pop stack))
       :else (conj stack c))))
  []
  form))

(defn parens-balanced?
 [form]
 (let [result (process-parens form)]
   (and (not (keyword? result)) (empty? result)))) 

(defn handle-input
 [cmd]
 (new-promise
  (fn [resolve]
    (let [{:keys [input server host port]} @repl]
      (swap! input str cmd)
      (if (parens-balanced? @input)
        (do
          (.send server @input port host)
          (reset! input ""))
        (println "Unbalanced parens: " cmd " - waiting for more"))
      (resolve true)))))

(defn process-msg
  [msg]
  (-> msg 
      (.toString) 
      (.split "\n")
      ((juxt #(.pop %) #(.join % "\n")))))

(defn handle-response
 [output msg rinfo]
 (let [[prompt result] (process-msg msg)]
  (.appendLine output result) 
  (.append output prompt)))

(def repl-init
 (quote 
    (binding [*warn-on-reflection* false]
        (do 
            (println "; Arcadia REPL")
            (println (str "; Clojure " (clojure-version)))
            (println (str "; Unity " (UnityEditorInternal.InternalEditorUtility/GetFullUnityVersion)))
            (println (str "; Mono " (.Invoke (.GetMethod Mono.Runtime "GetDisplayName" (enum-or BindingFlags/NonPublic BindingFlags/Static)) nil nil)))))))

(defn connect-repl 
 [output host port]
 (let [server (.createSocket dgram "udp4")]
  (.on server "error" #(println "Server error: " (js->clj %)))
  (.on server "message" (partial handle-response output))
  (.send server (str repl-init) port host)
  server))

(defn start-repl
  []
  (if @repl
    (resolved-promise true) ;; REPL already exists 
    (new-promise
      (fn [resolve]
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
             :port port})
          (resolve true))))))

(defn send 
  [msg]
  (when @repl
    (->
      (handle-input msg)
      (then (.appendLine (:output @repl) msg)))))

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
  (send editor (.. editor -document getText)))

(defn activate-repl
  []
  (println "Registering REPL commands")
  [ (register-command! "arcadia.replStart" start-repl)
    (register-text-editor-command! "arcadia.replSendLine" send-line)
    (register-text-editor-command! "arcadia.replSendSelection" send-selection)
    (register-text-editor-command! "arcadia.replSendFile" send-file)])