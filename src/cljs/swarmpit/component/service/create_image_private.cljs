(ns swarmpit.component.service.create-image-private
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :service :wizard :image :private])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defn- render-item
  [item]
  (val item))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- repository-handler
  [user]
  (handler/get
    (routes/path-for-backend :dockerhub-user-repo {:user user})
    {:on-call    (state/update-value [:searching] true cursor)
     :on-success (fn [response]
                   (state/update-value [:searching] false cursor)
                   (state/update-value [:data] response cursor))
     :on-error   (fn [_]
                   (state/update-value [:searching] false cursor))}))

(defn- form-username [user users]
  (comp/form-comp
    "DOCKER USER"
    (comp/select-field
      {:value    user
       :onChange (fn [_ _ v]
                   (state/update-value [:data] [] cursor)
                   (state/update-value [:user] v cursor)
                   (repository-handler v))}
      (->> users
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- form-repository [repository]
  (comp/form-comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Filter by name"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor))})))

(rum/defc form-loading < rum/static []
  (comp/form-comp-loading true))

(rum/defc form-loaded < rum/static []
  (comp/form-comp-loading false))

(defn- repository-list [data user]
  (let [repository (fn [index] (:name (nth data index)))]
    (comp/mui
      (comp/table
        {:key         "tbl"
         :selectable  false
         :onCellClick (fn [i]
                        (dispatch!
                          (routes/path-for-frontend :service-create-config
                                                    {}
                                                    {:repository (repository i)
                                                     :user       user
                                                     :registry   "dockerhub"})))}
        (comp/list-table-header headers)
        (comp/list-table-body headers
                              data
                              render-item
                              [[:name] [:description]])))))

(rum/defc form < rum/reactive [users]
  (let [{:keys [searching
                user
                data
                repository]} (state/react cursor)
        filtered-data (filter-items data repository)]
    (if (some? user)
      [:div.form-edit
       (form-username user users)
       (form-repository repository)
       [:div.form-edit-loader
        (if searching
          (form-loading)
          (form-loaded))
        (repository-list filtered-data user)]]
      [:div.form-edit
       (if (storage/admin?)
         (comp/form-icon-value icon/info [:span "No dockerhub users found. Add new " [:a {:href (routes/path-for-frontend :dockerhub-user-create)} "user."]])
         (comp/form-icon-value icon/info "No dockerhub users found. Please ask your admin to setup."))])))