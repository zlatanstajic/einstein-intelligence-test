# Windows signing setup

The MSI is currently unsigned. Trusted signing cannot start until a signing
provider approves the project and supplies credentials. The workflow includes
an integration for [SignPath Foundation’s free open source service](https://signpath.org/).
A self-signed certificate would not establish publisher trust on players’ PCs.

## Enrollment

1. The repository owner applies at <https://signpath.org/apply.html> and reviews
   the [eligibility and signing terms](https://signpath.org/terms.html).
2. After approval, connect the repository to SignPath and configure a project
   with a production signing policy. The uploaded artifact is a ZIP containing
   one MSI; the artifact configuration must sign that MSI and preserve its filename.
3. Publish the required code signing policy and attribution on the home/download
   page when enrollment becomes active. Identify committers, reviewers and signing
   approvers, and describe privacy: the game stores winners locally and opens
   external contact links only when requested by the player.
4. Configure repository Actions variables `SIGNPATH_ORGANIZATION_ID`,
   `SIGNPATH_PROJECT_SLUG` and `SIGNPATH_SIGNING_POLICY_SLUG`, and the Actions secret
   `SIGNPATH_API_TOKEN`. Enable the organization variable last, after the policy,
   artifact configuration and other settings are ready.
5. Push a release tag such as `v1.0.0` and approve the signing request in SignPath.
   The workflow waits up to 20 minutes for completion, checks the returned MSI’s
   Authenticode signature, and publishes the signed installer. Failure blocks the
   release; there is no unsigned fallback when signing is configured.

Pull requests and branch builds never submit signing requests. Without the
organization variable, builds produce unsigned installers. The release job
publishes the verified JAR, the final MSI and `SHA256SUMS.txt`.

The integration follows the [SignPath GitHub documentation](https://docs.signpath.io/trusted-build-systems/github).
It cannot be validated end to end before enrollment and credentials are available.
