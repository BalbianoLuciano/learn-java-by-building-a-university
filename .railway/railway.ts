// Railway infrastructure as code (docs/DEPLOY.md): one project, two services built from
// the Dockerfiles of the repo. Secrets stay in Railway (preserve()), never here.
import { defineRailway, preserve, project, service } from "railway/iac";

export default defineRailway(() => {
  // Uploaded with `railway up services/runner --path-as-root`: its Dockerfile is at the root.
  const runner = service("runner", {
    healthcheck: "/health",
    healthcheckTimeout: 120,
    replicas: { "us-east4-eqdc4a": 1 },
    env: { PORT: "8081", RUNNER_TOKEN: preserve() },
  });
  // Uploaded from the root of the repo: the Dockerfile needs the content and the contracts.
  const api = service("api", {
    healthcheck: "/api/v1/health",
    healthcheckTimeout: 120,
    replicas: { "us-east4-eqdc4a": 1 },
    env: {
      RAILWAY_DOCKERFILE_PATH: "services/api/Dockerfile",
      RUNNER_URL: "http://runner.railway.internal:8081",
      RUNNER_TOKEN: preserve(),
      ALLOWED_ORIGINS: preserve(),
    },
  });

  return project("ljbu", {
    resources: [runner, api],
  });
});
