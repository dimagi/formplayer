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

Edit the [formplayer-staging.yml](https://github.com/dimagi/staging-branches/blob/main/formplayer-staging.yml) file directly in staging-branches to include the branch you want on staging.

Rebuild the autostaging branch to be up-to-date:

```bash
./scripts/rebuildstaging --deploy
```
