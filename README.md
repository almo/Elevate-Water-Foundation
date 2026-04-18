# Elevated-Water-Index
Elevate Water Index (EWI): A multi-scale water risk platform. Using Kotlin/Ktor &amp; Python/FastAPI with Google Earth Engine, it translates complex hydrological data into actionable scores for facility capex, corporate ESG reporting, and basin-level strategic planning. Featuring dynamic geospatial mapping and financial-grade risk decomposition.

# Production Setting

## Development Enviroment


## Google Cloud
* Create a new project
* Create an AppEngine application
* Default service account:  elevate-water-foundation@appspot.gserviceaccount.com
* Grant the Storage Object Admin (roles/storage.objectAdmin) to the service account.
  This is needed to deploy AppEngine 
* Setup Firebase project binded to AppEngine Google Cloud Project. 
  * Pricing plan: Blaze
  * Configure Google Analytics
  * Adding Web App

*  Authentication
   *   Sign-in with Google
   *   Setup Authorize Domains
   *   Upgrade to GCIP (Identity Platform)
   *   Service Account (AppEngine and Firebase), permission 
       Cloud Datastore User & Cloud Run Invoker & Logs Writer


*   Artifacts Registry: setup artifacts delete policy


## Github
Setting up actions.
Setup signature for commits
