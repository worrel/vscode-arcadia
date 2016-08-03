(defproject vscode-arcadia "0.1.0-SNAPSHOT"
  :description "Arcadia extension for VSCode"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.6.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]]

  :plugins [[lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {
              :builds [{
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :main "arcadia.vscode.core" 
                                   :target :nodejs
                                   :output-wrapper true
                                   :output-to "extension.js"
                                   :optimizations :simple
                                   :pretty-print true}}]})


