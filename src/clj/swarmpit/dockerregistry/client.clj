(ns swarmpit.dockerregistry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string parse-stream]])
  (:import (org.httpkit BytesInputStream)))

(def ^:private base-url "https://index.docker.io/v2")

(defn- parse-response-body
  [body]
  (if (instance? BytesInputStream body)
    (parse-stream (clojure.java.io/reader body))
    (parse-string body true)))

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Docker registry client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-response-body body)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Docker registry error!"
                     {:status status
                      :body   response})))))))

(defn- get
  [api token headers params]
  (let [url (str base-url api)
        options {:headers      (merge headers
                                      {"Authorization" (str "Bearer " token)})
                 :query-params params}]
    (execute @(http/get url options))))

(defn tags
  [token repository]
  (let [api (str "/" repository "/tags/list")]
    (get api token {} nil)))

(defn manifest
  [token repository-name repository-tag]
  (let [api (str "/" repository-name "/manifests/" repository-tag)]
    (get api token nil nil)))

(defn distribution
  [token repository-name repository-tag]
  (let [api (str "/" repository-name "/manifests/" repository-tag)]
    (get api token {"Accept" "application/vnd.docker.distribution.manifest.v2+json"} nil)))
