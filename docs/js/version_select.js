window.addEventListener("DOMContentLoaded", function() {
  var BASE_URL = window.location.origin + "/" + window.location.pathname.split("/")[1];
  var CURRENT_VERSION = window.location.pathname.split("/")[3];
  var CURRENT_LANGUAGE = window.location.pathname.split("/")[2];

  function makeSelect(options, selected) {
    var select = document.createElement("select");
    select.classList.add("form-control");

    options.forEach(function(i) {
      var option = new Option(i.text, i.value, undefined,
                              i.value === selected);
      select.add(option);
    });

    return select;
  }

  var xhr = new XMLHttpRequest();
  xhr.open("GET", BASE_URL + "/versions.json");
  xhr.onload = function() {
    var versions = JSON.parse(this.responseText);

    var realVersion = versions.find(function(i) {
      return i.version === CURRENT_VERSION ||
             i.aliases.includes(CURRENT_VERSION);
    }).version;

    var select = makeSelect(versions.map(function(i) {
      return {text: i.title, value: i.version};
    }), realVersion);
    select.addEventListener("change", function(event) {
      window.location.href = BASE_URL + "/" + CURRENT_LANGUAGE + "/" + this.value;
    });

    var container = document.createElement("div");
    container.id = "version-selector";
    container.appendChild(select);

    var title = document.querySelector("nav.md-header-nav");
    var height = window.getComputedStyle(title).getPropertyValue("height");
    container.style.height = height;

    title.appendChild(container);
  };
  xhr.send();
});