# Contributing

Here are some ways that you can help contribute to this project.

## New ideas or Bug Reports

Need something? Found a bug? Or just have a brilliant idea? Head to the [Issues](https://github.com/GeyserMC/Geyser/issues) and create a new one.

Please feel free to reach out to us on [Discord](http://discord.geysermc.org/) if
you're interested in helping out with Geyser.

## Contributing Code

If you know Java then take a look at open issues and create a pull request.

Do the following to build the code:

```shell
git clone https://github.com/GeyserMC/Geyser
cd Geyser
git submodule update --init --recursive
mvn clean install
```

## Contributing Documentation

If you can help improve the documentation it would be highly appreciated. Have a look under the `docs` folder for the existing documentation.

The documentation is built using `mkdocs`. You can set up a hot-build dev environment that will auto-refresh changes as they are made.

### Requirements

* python3
* pip3
* npm (only if changing themes)

Install dependencies by running:

```shell
pip3 install -r requirements.txt
```

!!! note
    It's recommended to use a `virtualenv` so that you don't pollute your system environment.

### Dev Environment

To start a http document server on `http://127.0.0.1:8000` execute:

```shell
mkdocs serve
```

### Change PDF Theme

Edit the PDF theme under `docs/theme/pdf`. Rebuild by doing the following:

```shell
cd docs/theme/pdf
npm install
npm run build-compressed
```

This will update `pdf.css` under `docs/css/pdf.css`. Rebuilding the docs will now use the new theme.
