function loadNotFoundTemplate() {
    document.write(`
        <script id="not-found-template" type="text/handlebars-template">
            <p> NOT FOUND : {{path}}</p>
        </script>
    `);
}

loadNotFoundTemplate()