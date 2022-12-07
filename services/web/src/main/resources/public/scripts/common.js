/**
 * This will render a page
 * @param data that will populate the Handlebar variables
 * @param templateId of the template to be used
 * @param elementId the target element to be replaced with this template
 */
export function renderPage(data, templateId, elementId) {
    console.log(data)
    const template = document.getElementById(templateId).innerText;
    const compiledFunction = Handlebars.compile(template);
    document.getElementById(elementId).innerHTML = compiledFunction(data);
}

export function navigateTo(URL){
    window.location = "/#" + URL;

}



/**
 * Creates a promise that will take an accept and reject function
 * The accept function using the json of the response will be run if the response is ok
 * The reject will be run if the response is not ok
 * @param response from a fetch
 * @returns {Promise<any>}
 */
export function handleResponse(response){
    return new Promise(function(accept, reject) {
        if (response.status < 400) {
            accept(response.json());
        } else {
            if (reject === null){
                error(response, "app");
            } else {
                reject(response);
            }
        }
    });
}

/**
 * Renders and error page
 * @param response of the error
 * @param elementId to be overwritten
 */
export function error(response, elementId){
    const data = {status: response.status, message: response.statusText};
    const templateId = "error-template";
    console.log(data);
    renderPage(data, templateId, elementId);
}
