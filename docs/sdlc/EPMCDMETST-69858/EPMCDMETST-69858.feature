@EPMCDMETST-69858
Feature: Paginated product listing GET /api/products

  Background:
    Given the catalog contains at least 25 products across 3 categories

  @AC-1
  Scenario: First page of ten
    When I GET "/api/products?page=0&size=10"
    Then the status is 200
    And "items" has at most 10 products
    And "page" is 0 and "size" is 10
    And "totalItems" is greater than 10 and "totalPages" is greater than 1

  @AC-2
  Scenario: Third page
    When I GET "/api/products?page=2&size=10"
    Then the status is 200
    And "items" has at most 10 products
    And "page" is 2

  @AC-3
  Scenario: Negative page is rejected
    When I GET "/api/products?page=-1&size=10"
    Then the status is 400

  @AC-4
  Scenario: Zero size is rejected
    When I GET "/api/products?page=0&size=0"
    Then the status is 400

  @AC-5
  Scenario: Defaults apply when no parameters are given
    When I GET "/api/products"
    Then the status is 200
    And the body is an object with "items", "page", "size", "totalItems", "totalPages"
    And "page" is 0 and "size" is 10

  @AC-6
  Scenario Outline: Maximum page size is 100
    When I GET "/api/products?size=<size>"
    Then the status is <status>

    Examples:
      | size | status |
      | 100  | 200    |
      | 101  | 400    |

  @AC-7
  Scenario: Page past the end returns an empty envelope
    When I GET "/api/products?page=50&size=10"
    Then the status is 200
    And "items" is empty
    And "totalItems" and "totalPages" reflect the whole catalog

  @AC-8
  Scenario Outline: Non-numeric parameters are rejected
    When I GET "/api/products?<param>=abc"
    Then the status is 400

    Examples:
      | param |
      | page  |
      | size  |

  @AC-9
  Scenario: Items are ordered by id ascending across pages
    When I walk every page with size 10
    Then ids are strictly ascending and no id repeats

  @AC-10
  Scenario: Seeded dataset size
    Then the catalog has at least 25 products in the 3 existing categories

  @edge
  Scenario: Negative size and offset overflow
    When I GET "/api/products?size=-1"
    Then the status is 400
    When I GET "/api/products?page=2147483647&size=10"
    Then the status is 200 and "items" is empty

  @edge
  Scenario: Category endpoint stays an unpaginated array
    When I GET "/api/products/category/1"
    Then the body is a JSON array
