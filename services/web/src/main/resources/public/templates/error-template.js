function loadErrorTemplate() {
    document.write(`
        <script id="error-template" type="text/handlebars-template">
            <p>{{error code}} : {{message}}</p>
        </script>
    `);
}

loadErrorTemplate();