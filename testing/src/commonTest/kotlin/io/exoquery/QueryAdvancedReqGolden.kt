package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr

object QueryAdvancedReqGolden : GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "select clause + join + nested filters" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.ownerId,
        a.street,
        a.city
      FROM
        Person p
        INNER JOIN (
          SELECT
            a.ownerId,
            a.street,
            a.city
          FROM
            Address a
          WHERE
            a.street = '123 St.'
        ) AS a ON a.ownerId = p.id
      WHERE
        p.age > 18
      """
    ),
    "select clauses from(nested)" to cr(
      """
      SELECT
        r.ownerId,
        r.name,
        r.model
      FROM
        Person p
        INNER JOIN Address a ON a.ownerId = p.id
        INNER JOIN Robot r ON r.ownerId = p.id
      """
    ),
    "select clauses with join(select-clause)" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        r.first_ownerId AS ownerId,
        r.first_street AS street,
        r.first_city AS city,
        r.second_ownerId AS ownerId,
        r.second_name AS name,
        r.second_model AS model
      FROM
        Person p
        INNER JOIN (
          SELECT
            a.ownerId AS first_ownerId,
            a.street AS first_street,
            a.city AS first_city,
            r.ownerId AS second_ownerId,
            r.name AS second_name,
            r.model AS second_model
          FROM
            Address a
            INNER JOIN Robot r ON r.ownerId = p.id
            AND r.ownerId = a.ownerId
        ) AS r ON r.first_ownerId = p.id
      """
    ),
    "select clauses from(select-clause) + join(select-clause)" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        r.ownerId,
        r.name,
        r.model,
        a.first_id AS id,
        a.first_name AS name,
        a.first_age AS age,
        a.second_ownerId AS ownerId,
        a.second_street AS street,
        a.second_city AS city
      FROM
        Person p
        INNER JOIN Robot r ON r.ownerId = p.id
        INNER JOIN (
          SELECT
            p.id AS first_id,
            p.name AS first_name,
            p.age AS first_age,
            a.ownerId AS second_ownerId,
            a.street AS second_street,
            a.city AS second_city
          FROM
            Person p
            INNER JOIN Address a ON a.ownerId = p.id
          WHERE
            p.name = 'Jim'
        ) AS a ON a.first_id = p.id
      WHERE
        p.name = 'Joe'
      """
    ),
    "select clauses from(person.map(Robot)) + join" to cr(
      """
      SELECT
        p.id AS ownerId,
        p.name,
        p.name AS model,
        a.ownerId,
        a.street,
        a.city
      FROM
        Person p
        INNER JOIN Address a ON a.ownerId = p.id
      """
    ),
    "select clauses join(sql+map)" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        r.value AS second
      FROM
        Person p
        INNER JOIN (
          SELECT
            r.ownerId AS value
          FROM
            Robot r
        ) AS r ON r.value = p.id
      """
    ),
    "sql + select-clause + filters afterward" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.ownerId,
        a.street,
        a.city,
        r.ownerId,
        r.name,
        r.model
      FROM
        Person p,
        Address a
        INNER JOIN Robot r ON r.ownerId = p.id
      WHERE
        a.ownerId = p.id
      """
    ),
    "sql + select-clause (+nested) + filters afterward" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.ownerId,
        a.street,
        a.city,
        r.ownerId,
        r.name,
        r.model
      FROM
        Person p,
        (
          SELECT
            a.ownerId,
            a.street,
            a.city
          FROM
            Address a
          WHERE
            a.ownerId = p.id
        ) AS a
        INNER JOIN Robot r ON r.ownerId = p.id
      """
    ),
    "multiple from-clauses - no filters" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.ownerId,
        a.street,
        a.city
      FROM
        Person p,
        Address a
      """
    ),
    "multiple from-clauses - filter on 2nd" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.ownerId,
        a.street,
        a.city
      FROM
        Person p,
        Address a
      WHERE
        a.ownerId = p.id
      """
    ),
    "multiple from-clauses - filter on where" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.ownerId,
        a.street,
        a.city
      FROM
        Person p,
        Address a
      WHERE
        p.id = a.ownerId
      """
    ),
  )
}
