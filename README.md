# life-aggregator
A daily/weekly aggregator of useful information from various apps.

Below is a screenshot of the step function (stood up by my private terraform repo) that uses the code in this repo.
![Screenshot_20240621_170934](https://github.com/CSGrimey/life-aggregator/assets/2895932/a8d69e59-431b-4c52-9b20-c3517ae66f16)

Every day this workflow is invoked by an events bridge scheduler trigger, which ends with an email sent via AWS SES including a summary of relevant Google calendar events and incomplete Todoist tasks.

The Todoist api key and the Google service account credentials are stored in AWS param store.
Each integration lambda gets its relevant encrypted keys\creds via an env var that is injected by the [AWS param store lambda extension layer](https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html), which caches the values.
The original plan was to use AWS secrets manager, but unfortunately that does not go under the AWS free tier, whereas AWS param store does, but needs a little bit of extra work to use in the same way.

All lambdas are deployed via Terraform if any of their code hashes have changed after an sbt assembly has been executed locally.
All infra is stood up by Terraform, including components that are outside of the step function's scope (e.g. the events bridge scheduler trigger, the param store lambda layers, VPC, internet gateways etc)
