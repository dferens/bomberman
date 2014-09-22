(defproject bomberman "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [figwheel "0.1.4-SNAPSHOT"]
                 [reagent "0.4.2"]]
  :bower-dependencies [[react]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-figwheel "0.1.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "bomberman"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/compiled/bomberman.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :preamble ["reagent/react.js"]
                                   :optimizations :none
                                   :pretty-print true}}]})
