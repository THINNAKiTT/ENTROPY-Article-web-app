/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/webapp/**/*.{xhtml,html,js}",
    "./src/main/resources/META-INF/resources/**/*.{xhtml,html,js}"
  ],
  corePlugins: {
    preflight: false, // Recommended: prevents breaking PrimeFaces widgets
  },
  theme: {
    extend: {},
  },
  plugins: [],
}