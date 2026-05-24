# Overview
This project will take a Documentation Driven Development approach, in which the end-user documentation is written as if the software already exists, and is then used (by humans or agents) as directions to create the software.

# Project Documentation
- See README.md

# Architecture
- See ARCHITECTURE.md

# Next Steps
- See TODO.md

# Coding Practices
Project requirements are defined by the end-user documentation. To implement these requirements:
* First, before any code is written for a new feature or requirement, write automated tests that verify the behavior outlined in the end-user documentation. Initially, these tests will fail.
  * These tests will not be the only automated tests, so they should be easily identifiable by a TDD_ prefix in the test name, and commented with the line(s) in the documentation covered by this test
* Implement the new feature(s)
* Once the feature is implemented, the documentation tests will pass, and keeping those tests will prevent regressions.
* Always note next steps for implementation, if any, in TODO.md

# Guidelines
* Update code comments when relevant changes are made to the code
* Keep ARCHITECTURE.md current
* Use best practices relating to data security
* Use best practices relating to accessibility
* Prefer performant and battery-conscious solutions
* Prefer human-readability over source code brevity, both in structure and in naming
* Code should be modular and reused wherever possible, rather than duplicated. Common patterns should be abstracted out to short reusable helper functions
* Avoid defining "magic numbers" or string constants in the code which could be system settings or config variables
* Config variables containing secrets must not be copied to committed code
* Reuse existing structures, functions, and patterns when writing new features. If existing structures don't support needed behavior, prefer refactoring those structures to add support over parallelizing or short-circuiting existing structures
* Code should be easy to deploy, and must provide a path to roll back
* Use open standards whenever possible

## Dependencies
* 3rd party dependencies must be kept current
* Avoid introducing new dependencies to production code
* Dependencies must be removed when no longer needed

## Logging
* Always log key events for system visibility
* Always use the appropriate log level
  * Errors should be reserved for system-level problems that represent an unexpected outage or partial loss of functionality, which may require developer attention to address
  * Use Warnings for events that are unexpected, not optimal, and/or poorly handled, but that do not represent a system outage or loss of functionality that a user would notice.
  * Info should be used to enbable things like counting, tracking, or monitoring performance, and general system activity audits
  * Debug should be saved for verbose logs that are usually not wanted unless there is a problem that requires temporary in-depth troubleshooting
* Changing log level must be achieveable via a settings change, rather than a code deploy

## Automated Testing
* Write tests to ensure adherence to the end-user documentation, to uncover bugs in existing code, and to prevent future regressions
* When tests fail, start by looking for bugs in the code covered by the test
* Automated tests must mock everything that may contact external services, including the local database
* All potentially destructive code (code that could delete or overwrite existing data) must have test coverage
* All code that could possibly handle a user's Personally Identifiable Information (PII) must have test coverage
* All code initiating calls to external services must have test coverage
* Examine test run output for errors and warnings, and address them appropriately
  * If they are warnings or errors we are intentionally throwing or expecting as part of the test, try to catch them gracefully before they make it to test output
  * If they are errors or warnings thrown by the test infrastructure, or unexpected messages from the code we are testing, diagnose and address the underlying issue being described
* Test the things we expect to happen. Also test things like edge cases, missing resources, garbage inputs, and successful prevention of things we don't want to happen.


## Refactoring
* Code should occasionally be refactored to:
  * Comply with new requirements or objectives
  * Simplify existing code
  * Improve performance
  * Remove or update old dependencies
  * Remove deprecated features and general cruft
* Keep code refactoring work separate from feature development
  * When new features require refactoring, do the required refactor as a separate prep commit before working on the new feature directly
* Refactoring should be targeted, with individual refactoring commits confined to one or two improvements
* When refactoring, first make sure the targeted code has thorough test coverage for all expected behavior in that part of the system. After the refactor, reuse those tests to verify that the refactor does not change any expected system behavior
  * It is not unusual to uncover and fix pre-existing bugs as part of this process. These should be documented in the commit message

# Tagging a new release
Prior to tagging a new release, ensure that we are adhering to our own rules. Make and work through tasks to do the following:
* Look through the README.md file and compare the contents to the current code.
  * Identify areas of the code that need more end-user documentation
  * Identify parts of README.md that need to be corrected
  * Leave descriptive placeholders in square brackets in the README file, containing a short description of the fixes or undocumented behaviors that must be addressed.
  * Code behaviors that are currently tested in the TDDs without related information in the README.md should be prioritized.
  * Wait for the user to fix README.md before continuing to the next step.
* Have a look through the ARCHITECTURE.md, DESIGN.md, and TODO.md files, and call out any places where the documentation doesn't match the code. Decide interactively with the user which side is more correct in each mismatch case, and change the other side to match.
* If there are any substantial items in the README.md, DESIGN.md, or ARCHITECTURE.md docs that don't have TDD tests, write and run those tests which verify accuracy of the documentation.
* Remove completely finished sections from TODO.md. Leave only sections that still have unfinished pieces.
