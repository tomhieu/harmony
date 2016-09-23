-- Manage bookables and availability plans

-- :name insert-bookable :! :n 
-- :doc Insert a new bookable
insert into bookables (id, marketplace_id, ref_id, author_id, unit_type, active_plan_id)
values (:id, :marketplaceId, :refId, :authorId, :unitType, :activePlanId)

-- :name bookable-by-ref-spec-cols :? :1
-- :doc Get a bookable by referenced id and marketplace id
select :i*:cols from bookables
where marketplace_id = :marketplaceId AND ref_id = :refId

-- :name count-bookables-by-ref :? :1
-- :doc Return the count of bookables for given marketplace id and ref id. Possible values are 1 and 0.
select count(id) as count from bookables
where marketplace_id = :marketplaceId AND ref_id = :refId

-- :name insert-plan :! :n
-- :doc Insert a new plan
insert into plans (id, marketplace_id, bookable_id, seats, plan_mode)
values (:id, :marketplaceId, :bookableId, :seats, :planMode)

-- :name plan-by-id-spec-cols :? :1
-- :doc Get a plan by id
select :i*:cols from plans
where id = :id
