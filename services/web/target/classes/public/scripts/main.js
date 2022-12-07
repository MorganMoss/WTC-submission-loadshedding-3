import {handleResponse} from "./common.js";

let stage = 0;
let description = "";

const options = {
    method: 'GET'
}

function getStage() {
    fetch("url/stage")
        .then(handleResponse)
        .then(url =>
            fetch(url +"/stage", options)
                .then(handleResponse)
                .then(data => {
                    console.log(data)
                    stage =  data["stage"];
                    description = data["description"];
                    document.getElementById('stage').innerText = String(stage);
                    document.getElementById('description').innerText =description ;
                })
    )
}

function fillTable(data) {
    const schedule = document.getElementById("content");
    schedule.innerHTML = ""
    let start = data['startDate']
    let day = new Date(start[0], start[1], start[2])

    $.each(data["days"], function (i, object) {
        schedule.appendChild(document.createElement("br"));

        let tbl = document.createElement("table");
        let tblBody = document.createElement("tbody");
        let row = document.createElement("tr");



        let cell = document.createElement("th");
        let cellText = document.createTextNode(day.toLocaleDateString());
        day.setDate(day.getDate() + 1);
        cell.appendChild(cellText);
        row.appendChild(cell);
        tblBody.appendChild(row);


        $.each(object["slots"], function (j, startEnd) {
            let cell = document.createElement("td");
            let cellText = document.createTextNode(
                String(startEnd["start"][0]).padStart(2, "0") + ":" + String(startEnd["start"][1]).padStart(2, "0")
                + "-"
                + String(startEnd["end"][0]).padStart(2, "0") + ":" + String(startEnd["end"][1]).padStart(2, "0")
            );

            cell.appendChild(cellText);
            row.appendChild(cell);
        })

        tbl.appendChild(tblBody);
        schedule.appendChild(tbl);

        tbl.setAttribute("border", "2");

    })
}

function getSchedule(province, place){
    fetch("url/schedule")
        .then(handleResponse).then(url =>
            fetch(url + '/' + province + '/' + place , options )
                .then(handleResponse)
                .then(
                    data => {
                        console.log(data)
                        fillTable(data);
                    }
                )
    )
}

function fillPlaces(municipality) {
    fetch("url/places")
        .then(handleResponse).then(url =>
        fetch(url + '/places/municipality/' +municipality, options )
        .then(handleResponse)
        .then(
            data => {
                const dropdown = $('#places');
                dropdown.empty();
                dropdown.append('<option selected="true" disabled>Choose Place</option>');
                dropdown.prop('selectedIndex', 0);

                $.each(data, function (i, option) {
                    console.log(option.name)
                    dropdown.append(
                        $('<option/>')
                            .attr("value", option.name)
                            .text(option.name)
                    );
                });

                const province_select = document.getElementById("provinces")
                const place_select = document.getElementById("places");
                place_select.addEventListener('change',
                    event => getSchedule(
                        province_select.options[province_select.selectedIndex].value,
                        event.target.value
                    )
                )
            }, any => {}
        )
    )
    return null;
}

function fillMunicipalities(province) {
    const places = $('#places');
    places.empty()
    fetch("url/places")
        .then(handleResponse).then(url =>
            fetch(url + '/municipalities/' + province, options)
            .then(handleResponse)
            .then(
                data => {
                    const dropdown = $('#municipalities');
                    dropdown.empty();

                    dropdown.append('<option selected="true" disabled>Choose Municipality</option>');
                    dropdown.prop('selectedIndex', 0);

                    $.each(data, function (i, option) {
                        dropdown.append(
                            $('<option/>')
                                .attr("value", option.name)
                                .text(option.name)
                        );
                    });

                    const municipalities_select = document.getElementById('municipalities');
                    municipalities_select.addEventListener(
                        'change', event => fillPlaces(event.target.value)
                    );

                }, any => {}
                )
            )
    return null;
}

function fillProvinces(){
    fetch("url/places")
        .then(handleResponse).then(url =>
            fetch(url + `/provinces`, options)
            .then(handleResponse)
            .then(data => {


                const dropdown = $('#provinces');
                dropdown.empty();

                dropdown.append('<option selected="true" disabled>Choose Province</option>');
                dropdown.prop('selectedIndex', 0);

                $.each(data, function (i, option) {
                    console.log(option.name)
                    dropdown.append(
                        $('<option/>')
                            .attr("value", option.name)
                            .text(option.name)
                    );
                });

                const province_select = document.getElementById('provinces');
                province_select.addEventListener(
                    'change', event => fillMunicipalities(event.target.value)
                );

            })
        )
}

/**
 * This will set up the navbar and the element for content.
 * It redirects to log in if the id or email is null.
 */
export function main() {
    getStage()
    fillProvinces()
}
setInterval(getStage, 5000);

