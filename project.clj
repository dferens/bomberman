(defproject bomberman "0.1.0-SNAPSHOT"
  :clojurescript? true
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [figwheel "0.1.4-SNAPSHOT"]
                 [om "0.8.0-beta3"]
                 [sablono "0.2.22"]
                 [jayq "2.5.2"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-haml-sass "0.2.7-SNAPSHOT"]
            [lein-figwheel "0.1.4-SNAPSHOT"]
            [lein-bower "0.5.1"]]

  :bower-dependencies [[jquery "1.9"]
                       [flexboxgrid "5.0.0"]]

  :source-paths ["src"]
  :sass {:src "resources/public/sass"
         :output-directory "resources/public/css"
         :output-extension "css"}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/compiled/bomberman.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :optimizations :none
                                   :source-map "resources/public/js/compiled/bomberman.js.map"}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {:output-to "main.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js" "./externs/jquery-1.9.js"]}}]}
  :figwheel {:css-dirs ["resources/public/css"]})
