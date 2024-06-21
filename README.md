# life-aggregator
A daily/weekly aggregator of useful information from various apps.

Below is a screenshot of the step function (stood up by my private terraform repo) that uses the code in this repo.
![Screenshot_20240621_170934](https://github.com/CSGrimey/life-aggregator/assets/2895932/a8d69e59-431b-4c52-9b20-c3517ae66f16)

Every day this workflow is invoked by an events bridge scheduler trigger, which ends with an email sent via AWS SES including a summary of relevant Google calendar events and incomplete Todoist tasks.

The Todoist api key and the Google service account credentials are stored in AWS param store.
Each integration lambda gets its relevant encrypted keys\creds via an env var that is injected by the [AWS param store lambda extension layer](https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html), which caches the values.
