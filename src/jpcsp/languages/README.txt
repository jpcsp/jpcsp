* untranslated strings shall always be copied to all other locale files to ease translation
* all language files should be named jpcsp_<language>.properties or jpcsp_<language>_<country>.properties where:
  language is the lower-case two letter code for the language (e.g. pt for Portuguese)
  country is the upper case two letter code for the country (e.g. BR for Brazil)
* if a language has no regional specifics relevant for JPCSP then the country code shall be omitted (e.g. for Germany/Austria use 'de' only instead of 'de_DE' and 'de_AT')

This ensures that the proper locale will be loaded if the system locale is specified.
The user defineable settings from the menu should be adjusted to fit the available languages.

Locale codes shall be valid with regards to java.util.Locale.
