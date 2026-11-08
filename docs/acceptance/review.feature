# Acceptance specification. Automated evidence: tests/integration/local-completion.mjs
@US06 @US13
Feature: List report jobs for a unit

  Scenario: Authorized or healthy request
    Given the owner of a unit has an active plan and an Excel report job has completed
    When the client requests GET /api/v1/reports?unitId=<authorized-unit>
    Then the response status is 200
    And the response lists the job with status completed

  Scenario: Boundary or access control
    Given the review environment is available
    When the request omits the access token
    Then the response status is 401
    And report jobs are not disclosed
