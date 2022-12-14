# How to rebuild staging branch
Start by setting up dependencies.

Create or enter a python virtualenv and the code directory:
```bash
mkvirtualenv formplayer # or workon formplayer
cd formplayer
```

Install the python dependencies:
```bash
pip install -r scripts/rebuildstaging-requirements.txt
```

Edit this file to include the branch you want to be on staging and commit and push the change
```bash
vim ./scripts/staging.yaml
./scripts/commit-staging
```

Rebuild the autostaging branch to align with the spec in `./scripts/staging.yaml`:

```bash
./scripts/rebuildstaging --deploy
```
