import {renderPage} from "./common.js";

export function notFound(path){
    const templateId = "not-found-template";
    const elementId = "app";
    const data = {path: path}

    renderPage(data, templateId, elementId);
}