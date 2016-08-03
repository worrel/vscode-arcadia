(ns arcadia.vscode.core
  (:require [cljs.nodejs :as nodejs]
            [vscode.util :as util :refer [info-message register-command! push-subscription!]]
            [arcadia.vscode.repl :as repl]))

(nodejs/enable-util-print!)

(defn command
 []
 (info-message "Hello Arcadia"))
 
(defn push-all-subs
 [context f]
 (doseq [disp (f)]
  (push-subscription! context disp)))

(defn activate
 [context]
 (println "activating vscode-arcadia")
 (push-all-subs context repl/activate-repl))

(defn deactivate
 []
 (println "vscode-arcadia deactivated"))

(defn -main
 []
 (println "vscode-arcadia loaded"))

(util/export! {:activate activate})

(set! *main-cli-fn* -main)