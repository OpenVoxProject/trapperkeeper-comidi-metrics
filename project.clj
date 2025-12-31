(defproject org.openvoxproject/trapperkeeper-comidi-metrics "0.1.3-SNAPSHOT"
  :description "Comidi/HTTP Metrics for Trapperkeeper"
  :url "http://github.com/openvoxproject/trapperkeeper-comidi-metrics"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.txt"}

  :min-lein-version "2.9.1"

  :pedantic? :abort

  :parent-project {:coords [org.openvoxproject/clj-parent "7.5.1"]
                   :inherit [:managed-dependencies]}

  :dependencies [[org.clojure/clojure]
                 [prismatic/schema]
                 [org.openvoxproject/trapperkeeper-metrics]
                 [org.openvoxproject/comidi]]

  :plugins [[lein-parent "0.3.7"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.openvoxproject/trapperkeeper :classifier "test"]
                                  [org.openvoxproject/kitchensink :classifier "test"]
                                  [org.openvoxproject/trapperkeeper-status]
                                  [org.openvoxproject/http-client]
                                  [org.bouncycastle/bcpkix-jdk18on]
                                  [org.openvoxproject/trapperkeeper-webserver-jetty10]]}}

  :aliases {"example" ["run" "-m" "example.comidi-metrics-web-app"]
            "example-data" ["run" "-m" "example.traffic-generator"]})
