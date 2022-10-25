This sections explains some proposed guildelines for developing glisten

## Releases

Glisten is setup to make it pretty easy to create a new release.

Hence the main branch is restricted and can only be used using Pull Requests.

Each Pull Request will trigger a new release. 
Thus the PR will check if everything is setup (e.g. new version no.) so we can smoothly release.

The release will trigger that the mkdocs Documentation will also be build and copied to `gh-pages`. 

## In which branch to work?

The main developing branch is `develop` as `main` is declared as the release branch. 

However if multiple people working one one branch for different features might get annoying, we propose to use a new branch called `feature/MYFEATURE` and merge into `develop` using a PR when the code is tested and finished. 

## Code of Conduct & Contribution

Our Code of Conduct: [https://github.com/dice-group/glisten/blob/main/CODE_OF_CONDUCT.md](https://github.com/dice-group/glisten/blob/main/CODE_OF_CONDUCT.md)

Our Contributiosn Readme: [https://github.com/dice-group/glisten/blob/main/CONTRIBUTING.md](https://github.com/dice-group/glisten/blob/main/CONTRIBUTING.md)