(ns vscode.util)

(def vscode (js/require "vscode"))

(defn export!
  [exports]
  (set! (.-exports js/module) (clj->js exports)))

(defn register-command!
 [id f]
 (.registerCommand (.. vscode -commands) id f))

(defn register-text-editor-command!
 [id f]
 (.registerTextEditorCommand (.. vscode -commands) id f))

(defn push-subscription!
 [ctx disp]
 (.push (.. ctx -subscriptions) disp))

(defn info-message
 [msg]
 (.showInformationMessage (.. vscode -window) msg))

(defn get-config
 [path]
 (.. vscode -workspace (getConfiguration path)))
 
(defn resolved-promise
 [body]
 (.resolve js/Promise body))

(defn new-promise
 [f]
 (js/Promise. f))

(defn then
 [p f]
 (.then p f))