(ns swarmpit.test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [swarmpit.install :as install]))

(defn dind-socket-fixture
  [test]
  (swarmpit.config/update! {:docker-sock "http://localhost:12375"})
  (test))

(defn running-service-fixture
  [test]
  (let [id (-> (slurp "test/clj/swarmpit/docker/service.edn")
               (edn/read-string)
               (merge {:Name "test-service"})
               (swarmpit.docker.client/create-service)
               :ID)]
    (test)
    (swarmpit.docker.client/delete-service id)))

(defn db-init-fixture
  [test]
  (install/init)
  (test))