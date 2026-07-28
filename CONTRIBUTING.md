# Contributing

First off, thanks for taking the time to contribute!

Contributions are welcome, do not hesitate to open an issue, a pull request, etc...
For features, it's better to create an issue first, in order to get feedback on whether the feature is wanted or not, and to align on the best approach before starting development.

# Translation

The app is English only. Translations are not maintained here anymore: a
half translated interface reads worse than one that is honest about the single
language it speaks, and keeping five locales in step with the strings was work
nobody was doing. Please don't open pull requests adding locales.

# File extensions

You can add supported file extension by adding an extension in [this those files](./app/src/main/rust/supported_extensions). Make sure to call `just sort-supported-extension` after.
