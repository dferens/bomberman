(defproject bomberman "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [figwheel "0.1.4-SNAPSHOT"]
                 [reagent "0.4.2"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-haml-sass "0.2.7-SNAPSHOT"]
            [lein-figwheel "0.1.4-SNAPSHOT"]
            [lein-bower "0.5.1"]]

  :bower-dependencies [[react "0.9.0"]]

  :source-paths ["src"]
  :sass {:src "resources/public/sass"
         :output-directory "resources/public/css"
         :output-extension "css"}

  :cljsbuild {:builds [{:id "bomberman"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/compiled/bomberman.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :optimizations :none
                                   :pretty-print true}}]}
  :figwheel {:css-dirs ["resources/public/css"]})
