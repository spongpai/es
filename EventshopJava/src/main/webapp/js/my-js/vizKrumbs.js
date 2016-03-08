
$(function() {
    var formData = [];
    // Legend Container
    // Create a rainbow from a pretty color scheme.
    // http://www.colourlovers.com/palette/292482/Terra
    rainbow = new Rainbow();
    //rainbow.setSpectrum("#E8DDCB", "#CDB380", "#036564", "#033649", "#031634");
    rainbow.setSpectrum('#44CE00', '#84D200', '#C7D500', '#D9A700', '#DD6700', '#E12600', '#E5001E');
    buildLegend();


    // UI Elements - Creates Map, Location Auto-Complete, Selection Rectangle
    var mapOptions = {
        //center: new google.maps.LatLng(37.00, -120.00),
        //center: {lat: 37.090, lng: -95.712},
        center: {lat: 37.090, lng: -10.00},	// world
        center: {lat: 33.5, lng: -117.0},
        zoom: 8,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        streetViewControl: false,
        draggable : true,
        mapTypeControl: false
    };
    map = new google.maps.Map(document.getElementById('map_canvas'), mapOptions);

    var input = document.getElementById('location-text-box');
    var autocomplete = new google.maps.places.Autocomplete(input);
    autocomplete.bindTo('bounds', map);

    google.maps.event.addListener(autocomplete, 'place_changed', function() {
        var place = autocomplete.getPlace();
        if (place.geometry.viewport) {
            map.fitBounds(place.geometry.viewport);
        } else {
            map.setCenter(place.geometry.location);
            map.setZoom(17);  // Why 17? Because it looks good.
        }
        var address = '';
        if (place.address_components) {
            address = [(place.address_components[0] && place.address_components[0].short_name || ''),
                (place.address_components[1] && place.address_components[1].short_name || ''),
                (place.address_components[2] && place.address_components[2].short_name || '') ].join(' ');
        }
    });

    // Drawing Manager for selecting map regions. See documentation here:
    // https://developers.google.com/maps/documentation/javascript/reference#DrawingManager
    rectangleManager = new google.maps.drawing.DrawingManager({
        drawingMode : google.maps.drawing.OverlayType.RECTANGLE,
        drawingControl : false,
        rectangleOptions : {
            strokeWeight: 1,
            clickable: false,
            editable: true,
            strokeColor: "#2b3f8c",
            fillColor: "#2b3f8c",
            zIndex: 1
        }
    });
    rectangleManager.setMap(map);
    selectionRectangle = null;

    // Drawing Manager: Just one editable rectangle!
    google.maps.event.addListener(rectangleManager, 'rectanglecomplete', function(rectangle) {
        selectionRectangle = rectangle;
        rectangleManager.setDrawingMode(null);
    });


    // Initialize data structures
    map_cells = [];
    map_tweet_markers = [];
    map_info_windows = [];//{};

    initDemoUIButtonControls();
    onOpenExploreMap();

    //map.on('zoomend', resizeIconMarker);
    //on zoom
    google.maps.event.addListener(map, 'zoom_changed', function() {
        var new_map_cells = [];

        console.log(map.getZoom() + ", " + map_cells.length + ", " + 200*(20 - map.getZoom()));
        /*
         for(var i = 0; i < map_cells.length; i++){
         var marker = map_cells[i];
         //console.log(marker);
         marker.radius = 200*(20 - map.getZoom());
         //marker.setRadius(200*(20 - map.getZoom()));
         console.log(marker)
         new_map_cells.push(marker);
         }
         */
        new_map_cells = map_cells;
        mapWidgetClearMap();

        // create all markers on the map according to the mapPlotData object
        /*
         var p = Math.pow(2, (21 - map.getZoom()));
         //var radius = p * 1128.497220 * 0.0027;
         var radius = p * 628.497220 * 0.0027;
         for(var i = 0; i < new_map_cells.length; i++) {
         var map_circle = new google.maps.Circle({
         strokeColor: new_map_cells[i].strokeColor,
         strokeOpacity: new_map_cells[i].strokeOpacity,
         strokeWeight: new_map_cells[i].strokeWeight,
         fillColor: new_map_cells[i].fillColor,
         fillOpacity: new_map_cells[i].fillOpacity,
         map: map,
         center: new_map_cells[i].center,
         value: new_map_cells[i].value,
         fmblist: new_map_cells[i].fmblist,
         radius: radius//200 * (20 - map.getZoom())^2
         });
         //alert(map_circle.getRadius());
         map_cells.push(map_circle);
         }
         */
        for(var i = 0; i < new_map_cells.length; i++) {
            console.log('in zoom changed: ' + new_map_cells[i].paths);
            var map_poly = new google.maps.Polygon({
                paths: new_map_cells[i].gridpaths,
                gridpaths: new_map_cells[i].gridpaths,
                strokeColor: new_map_cells[i].strokeColor,
                strokeOpacity: new_map_cells[i].strokeOpacity,
                strokeWeight: new_map_cells[i].strokeWeight,
                fillColor: new_map_cells[i].fillColor,
                fillOpacity: new_map_cells[i].fillOpacity,
                map: map,
                center: new_map_cells[i].center,
                value: new_map_cells[i].value,
                fmblist: new_map_cells[i].fmblist
                //radius: radius//200 * (20 - map.getZoom())^2
            });
            //alert(map_circle.getRadius());
            map_cells.push(map_poly);
        }

        $("#rainbow-legend-container").show();
        addEventOnMarkers();
        //plotMarkerOnMap();
        //triggerUIUpdate(mapPlotData, maxWeight, minWeight, maxAggval, minAggval)
        //alert('radius: ' + 200*(20 - map.getZoom()));
        //plotMarkerOnMap();
    });
    function getLatLonRes(){
        /*
         if(map.getZoom() < 5)
         return '5.0, 5.0';
         else if(map.getZoom() < 8)
         return '2.0, 2.0';
         else if(map.getZoom() < 10)
         return '0.10, 0.10';
         else
         return '0.05, 0.05';
         */
        formData["gridLat"] = $("#gridlat").text();
        formData["gridLng"] = $("#gridlng").text();
        return formData["gridLat"] + ", " + formData["gridLng"];
    }


    var startTimer;
    var endTimer;
    //First, create an object containing LatLng and population for each city.
    var citymap = {
        chicago: {
            center: {lat: 41.878, lng: -87.629},
            population: 2714856
        },
        newyork: {
            center: {lat: 40.714, lng: -74.005},
            population: 8405837
        },
        losangeles: {
            center: {lat: 34.052, lng: -118.243},
            population: 3857799
        },
        vancouver: {
            center: {lat: 49.25, lng: -123.1},
            population: 603502
        }
    };
    function initDemoUIButtonControls() {
        // Explore Mode - Update Sliders
        var updateSliderDisplay = function(event, ui) {
            if (event.target.id == "grid-lat-slider") {
                $("#gridlat").text(""+ui.value);
            } else {
                $("#gridlng").text(""+ui.value);
            }
        };
        sliderOptions = {
            max: 10,
            min: 0.1,
            step: 0.1,
            value: 0.5,
            slidechange: updateSliderDisplay,
            slide: updateSliderDisplay,
            start: updateSliderDisplay,
            stop: updateSliderDisplay
        };
        $("#gridlat").text(""+sliderOptions.value);
        $("#gridlng").text(""+sliderOptions.value);
        $(".grid-slider").slider(sliderOptions);

        // Explore Mode - Query Builder Date Pickers
        var dateOptions = {
            dateFormat: "yy-mm-dd",
            defaultDate: "2015-12-01",
            navigationAsDateFormat: true,
            constrainInput: true
        };
        var start_dp = $("#start-date").datepicker(dateOptions);
        start_dp.val(dateOptions.defaultDate);
        dateOptions['defaultDate'] = "2016-01-01";
        var end_dp= $("#end-date").datepicker(dateOptions);
        end_dp.val(dateOptions.defaultDate);

        // Explore Mode: Toggle Selection/Location Search
        $('#selection-button').on('change', function (e) {
            $("#location-text-box").attr("disabled", "disabled");
            rectangleManager.setMap(map);
            if (selectionRectangle) {
                selectionRectangle.setMap(null);
                selectionRectangle = null;
            } else {
                rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
            }
        });
        $('#location-button').on('change', function (e) {

            $("#location-text-box").removeAttr("disabled");
            selectionRectangle.setMap(null);
            rectangleManager.setMap(null);
            rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);
        });
        $("#selection-button").trigger("click");

        // Explore Mode - Clear Button
        $("#clear-button").click(mapWidgetResetMap);

        // Explore Mode: Query Submission
        $("#submit-button").on("click", function () {

            $("#report-message").html('');
            //$("#submit-button").attr("disabled", true);
            rectangleManager.setDrawingMode(null);

            var dsname = $("#dsname").val();
            var startdp = $("#start-date").datepicker("getDate");
            var enddp = $("#end-date").datepicker("getDate");
            var startdt = $.datepicker.formatDate("yy-mm-dd", startdp)+"T00:00:00Z";
            var enddt = $.datepicker.formatDate("yy-mm-dd", enddp)+"T23:59:59Z";

            var formData = {
                "dsname": dsname,
                "startdt": startdt,
                "enddt": enddt,
                "gridlat": $("#grid-lat-slider").slider("value"),
                "gridlng": $("#grid-lng-slider").slider("value")
            };

            // Get Map Bounds
            var bounds;
            if ($('#selection-label').hasClass("active") && selectionRectangle) {
                bounds = selectionRectangle.getBounds();
            } else {
                bounds = map.getBounds();
            }
            console.log(bounds);

            var swLat = bounds.getSouthWest().lat();
            var swLng = bounds.getSouthWest().lng();
            var neLat = bounds.getNorthEast().lat();
            var neLng = bounds.getNorthEast().lng();

            formData["swLat"] = Math.min(swLat, neLat);
            formData["swLng"] = Math.min(swLng, neLng);
            formData["neLat"] = Math.max(swLat, neLat);
            formData["neLng"] = Math.max(swLng, neLng);
            console.log('after', swLat, ',' , swLng, ',' ,neLat,  ',', neLng);
            if(neLng < swLng){	// world is round!!!
                formData["swLng"] = "-180.0";
            }
            // Get theme
            // This following function doesn't work, when the multiple-selection list is empty in the beginning.
            // Don't know the reason.
            //var selectedValues = $('#theme-select').val();
            //alert(selectedValues)
            //$('#theme-select :selected').each(function(i, sel){
            //    alert( $(sel).val());
            //});
            var selectedTheme = []
            $('.sol-selected-display-item-text').each(function(i, obj) {
                //alert($(this).text());
                selectedTheme[i] = $(this).text();
            });
            if(selectedTheme.length == 0){
                //context:intent_id:intent_name
                formData["themeName"] = "null";
                formData["themeValue"] = "null";
            }
            else{
                // e.g., POPULAR, FOOD:3:Drinking
                for(var i = 0; i < selectedTheme.length; i++){
                    formData["themeValue"] = selectedTheme[i].replace(':','&');
                    var subTheme = selectedTheme[i].split('&');
                    if(subTheme.length == 1)
                        formData["themeName"] = 'context';
                    else
                        formData["themeName"] = 'context&intent_id&intent_name';
                }
            }

            var query = 'webresources/queryservice/asterixds/'+formData["dsname"]+'/'
                + formData["themeName"] + '/' + formData["themeValue"] + '/'+formData["swLat"]+','+formData["swLng"]
                + '/'+formData["neLat"]+','+formData["neLng"]
                + '/'+formData["startdt"]+'/'+formData["enddt"] + '/' + getLatLonRes() + '/emage';
            $('#emage-query').html(query);
            startTimer = performance.now()
            console.log(query);
            $.ajax( {
                url: query,
                type: 'GET',
                success: function( response ) {
                    //console.log(response);
                    // select as Emage
                    queryCallback(response);
                }
            } );
            // Clears selection rectangle on query execution, rather than waiting for another clear call.
            //if (selectionRectangle) {
            //    selectionRectangle.setMap(null);
            //    selectionRectangle = null;
            //}
        });
    }

    /**
     * A spatial data cleaning and mapping call
     * @param    {Object}    res, a result object from a tweetbook geospatial query
     */
    function queryCallback(res) {
        // First, we check if any results came back in.
        // If they didn't, return.
        if (res.length==0) {
            /** Clear anything currently on the map **/
            mapWidgetClearMap();

            reportUserMessage("Oops, no results found for those parameters.", false, "report-message");
            return;
        }

        endTimer = performance.now()
        //alert( (endTimer - startTimer) + ' miliseconds')
        $('#emage-duration').html(parseFloat(Math.round((endTimer-startTimer)) / 1000).toFixed(3) + ' seconds')


        // Initialize coordinates and weights, to store
        // coordinates of map cells and their weights
        // Parse resulting JSON objects.
        var minWeight = 99999, maxWeight = -99999, minAggval = 999999, maxAggval = -99999;
        var cells = [];

        $.each(res, function(i, data){

            var centerLat = (data.cell.rectangle[0].point[0] + data.cell.rectangle[1].point[0])/ 2.00000;
            var centerLng = (data.cell.rectangle[0].point[1] + data.cell.rectangle[1].point[1])/ 2.00000;

            var cell = {
                "center"	: {lat: centerLat, lng: centerLng},
                "gridpaths" : [{lat: data.cell.rectangle[0].point[0], lng: data.cell.rectangle[0].point[1]},
                    {lat: data.cell.rectangle[0].point[0], lng: data.cell.rectangle[1].point[1]},
                    {lat: data.cell.rectangle[1].point[0], lng: data.cell.rectangle[1].point[1]},
                    {lat: data.cell.rectangle[1].point[0], lng: data.cell.rectangle[0].point[1]},
                    {lat: data.cell.rectangle[0].point[0], lng: data.cell.rectangle[0].point[1]}],
                //"value"    	: data.count.int64,
                "value"    	: data.count,
                "list"  	: data.values.orderedlist
            };


            // We track the minimum and maximum weight to support our legend.
            maxWeight = Math.max(cell["value"], maxWeight);
            minWeight = Math.min(cell["value"], minWeight);

            maxAggval = Math.max(cell["value"], maxAggval);
            minAggval = Math.min(cell["value"], minAggval);

            $('#emage-output').append(cell["center"].lat + "," + cell["center"].lng + ", " + cell["value"] + "<br>");
            cells.push(cell);
        });


        triggerUIUpdate(cells, maxWeight, minWeight, maxAggval, minAggval);
    }

    /**
     * Triggers a map update based on a set of spatial query result cells
     * @param    [Array]     mapPlotData, an array of coordinate and weight objects
     * @param    [Array]     plotWeights, a list of weights of the spatial cells - e.g., number of tweets
     */
    function triggerUIUpdate(mapPlotData, maxWeight, minWeight, maxAggval, minAggval) {
        /** Clear anything currently on the map **/
        mapWidgetClearMap();
        //alert('trigger UI update');

        // Initialize info windows.
        //var map_info_windows = [];

        // Initialize markers.
        //var map_cells = []


        // Show legend based on the aggreated value
        $("#legend-min").html(minAggval);
        $("#legend-max").html(maxAggval);
        $("#rainbow-legend-container").show();
        /*
         var p = Math.pow(2, (21 - map.getZoom()));
         var radius = p * 628.497220 * 0.0027;

         // create all markers on the map according to the mapPlotData object
         for(var i = 0; i < mapPlotData.length; i++) {
         var map_circle = new google.maps.Circle({
         strokeColor: '#FF0000',
         strokeOpacity: 0.5,
         strokeWeight: 1,
         fillColor: "#" + rainbow.colourAt(Math.ceil(100 * (mapPlotData[i].value / maxAggval))),//'#FF0000',
         fillOpacity: 0.8,
         map: map,
         center: mapPlotData[i].center,
         value: mapPlotData[i].value,
         fmblist: mapPlotData[i].list,
         radius: radius //200*(20 - map.getZoom())//80000//Math.sqrt(mapPlotData[i].value) * 10000//mapWidgetComputeCircleRadius(mapPlotData[i], maxWeight)//200000//
         });
         map_cells.push(map_circle);
         }
         */
        for(var i = 0; i < mapPlotData.length; i++) {
            var map_polygon = new google.maps.Polygon({

                paths: [mapPlotData[i].gridpaths],
                gridpaths: mapPlotData[i].gridpaths,
                strokeOpacity: 0.5,
                strokeWeight: 1,
                fillColor: "#" + rainbow.colourAt(Math.ceil(100 * (mapPlotData[i].value / maxAggval))),//'#FF0000',
                fillOpacity: 0.8,
                map: map,
                center: mapPlotData[i].center,
                value: mapPlotData[i].value,
                fmblist: mapPlotData[i].list
            });
            map_cells.push(map_polygon);
        }

        addEventOnMarkers();


    }




    function addEventOnMarkers(){

        var infowindow = new google.maps.InfoWindow({
            content: "holding..."
        });
        // create map information and gallery popup event for each marker
        // NOTE: Using "this" instead of "marker" when create event fires function!!!
        // REF: http://you.arenot.me/2010/06/29/google-maps-api-v3-0-multiple-markers-multiple-infowindows/
        for(var i = 0; i < map_cells.length; i++){
            var marker = map_cells[i];
            // on click
            google.maps.event.addListener(marker, 'click', function(event) {

                var data = [];
                for(var j = 0; j < this.fmblist.length; j++){
                    var fmb = {
                        image: this.fmblist[j].stt_what.media_source.value,
                        title: this.fmblist[j].stt_what.caption.value,
                        link: this.fmblist[j].stt_what.media_source.value,
                        //description: marker.fmblist[j].stt_what.caption.value,
                        //link: this.fmblist[j].stt_what.k_url.value,
                        layer: '<div>' + this.fmblist[j].stt_what.caption.value + '</div>'
                    };
                    console.log(fmb);
                    data.push(fmb);
                }
                //console.log("fmb data: " + data);
                //console.log("fmb data lenght: " + data.length);
                Galleria.loadTheme('js/libs/galleria/themes/classic/galleria.classic.min.js');
                Galleria.run('.galleria', {
                    dataSource: data
                });
                //alert('mouse click')
                $('#photos').css({ 'width': '500px', 'height': '300px', 'background': '#000' });
                //$('#photos').galleria({ width: 500, height: 300 });
                //$("#dialog").dialog({modal: true });
                $("#dialog").dialog({modal: true, height: 400, width: 600 });
            });
            // on mouse over
            google.maps.event.addListener(marker, 'mouseover', function(event) {
                infowindow.setContent("<p>Number of pictures: " + this.value+ "</p>");
                infowindow.setPosition(this.center);
                infowindow.open(map, this);
            });

        }  // end for loop
    }
    /**
     * Explore mode: Initial map creation and screen alignment
     */
    function onOpenExploreMap () {
        //var explore_column_height = $('#explore-well').height();
        //var right_column_width = $('#right-col').width();
        var explore_column_height = 400; //$('#explore-well').height();
        var right_column_width =$('#right-col').width();
        $('#map_canvas').height(explore_column_height + "px");
        $('#map_canvas').width(right_column_width + "px");

        //$('#review-well').height(explore_column_height + "px");
        //$('#review-well').css('max-height', explore_column_height + "px");
        $('#right-col').height(explore_column_height + "px");

        setTimeLine();
        // Create the map.
        /*
         var map = new google.maps.Map(document.getElementById('map_canvas'), {
         zoom: 4,
         center: {lat: 37.090, lng: -95.712},
         mapTypeId: google.maps.MapTypeId.TERRAIN
         });
         */
    }


    function getShortDate(date){
        var m = date.getMonth() + 1;
        if(m < 10) m = '0' + m;
        var d = date.getDate();
        if(d < 10) d = '0' + d;
        var twoDigitsYear = date.getFullYear().toString().substr(2,2);
        return twoDigitsYear + "-" + m + "-" + d;
    }

    function getFormateDate(date){
        var m = date.getMonth() + 1;
        if(m < 10) m = '0' + m;
        var d = date.getDate();
        if(d < 10) d = '0' + d;
        return date.getFullYear() + "-" + m + "-" + d;
    }

    function setTimeLine(){
        // DOM element where the Timeline will be attached
        var container = document.getElementById('visualization');
        var options = {
            zoomMin: 1000 * 60 * 60 * 24 * 1,   // one day
            zoomMax: 1000 * 60 * 60 * 24 * 30 * 1  // one months
        };
        var startDate = new Date("2015-11-30T00:00:00");
        var step = 7;	// 1 week
        var endDate = new Date(); // today

        if(formData["dsname"] == "STTfmbtest4")
            startDate = new Date("2015-11-30T00:00:00");
        else if(formData["dsname"] == "STTfood_instagram"){
            startDate = new Date("2015-12-22T00:21:38");
            step = 2;
            endDate = new Date("2016-01-31T00:00:00");
        } else {
            startDate = new Date("2016-02-27T00:00:00");
            step = 1; // one day
        }
        var data = new vis.DataSet(options);

        // Create a DataSet
        var dataset = [];
        endDate.setDate(endDate.getDate() + 1);
        var count =1;
        dataset.push({id: 1, content: '<span style="font-size:small">ALL</span>', start: getFormateDate(startDate), end: getFormateDate(endDate)});

        while(startDate < endDate){
            var eDate = new Date(startDate.getTime());
            eDate.setDate(eDate.getDate() + step);
            var label = '<span style="font-size:small;font-size: 0.6em">' + getShortDate(startDate) + '+</span>';
            count++;
            dataset.push({id: count, content: label, start: getFormateDate(startDate), end: getFormateDate(eDate)});

            startDate.setDate(startDate.getDate() + step);
        }
        data.add(dataset);

        /*
         data.add([
         {id: 1, content: 'all', start: '2015-11-30', end: '2016-01-24'},
         {id: 2, content: 'week 1', start: '2015-11-30', end: '2015-12-06'},
         {id: 3, content: 'week 2', start: '2015-12-07', end: '2015-12-13'},
         {id: 4, content: 'week 3', start: '2015-12-14', end: '2015-12-20'},
         {id: 5, content: 'week 4', start: '2015-12-21', end: '2015-12-27'},
         {id: 6, content: 'week 5', start: '2015-12-28', end: '2016-01-03'},
         {id: 7, content: 'week 6', start: '2016-01-04', end: '2016-01-10'},
         {id: 8, content: 'week 7', start: '2016-01-11', end: '2016-01-17'},
         {id: 9, content: 'week 8', start: '2016-01-18', end: '2016-01-24'}
         ]);
         */
        //data.on('*', function(event, properties, senderId){
        //	  console.log('event', event, properties);
        //});

        // var item1 = data.get([1,18]);
        //console.log('item1', item1);

        // Create a Timeline
        var timeline = new vis.Timeline(container, data, options);

        timeline.on('select', function (properties) {
            var item = data.get(properties.items[0])
            var startdate = item.start+"T00:00:00Z";
            var enddate = item.end+"T00:00:00Z";
            var sDate = new Date(startdate);
            var eDate = new Date(enddate);
            alert('selected items: ' + sDate.getTime() + ', ' + eDate.getTime());

            $("#report-message").html('');
            //$("#submit-button").attr("disabled", true);
            rectangleManager.setDrawingMode(null);

            var formData = setParameter(startdate,enddate);
            //updateParameter(startdate,enddate, "", "", true, true);
            console.log('formData: ' + formData["startdt"] + ',' + formData["enddt"]);

            formData["dsname"] = 5;
            var query = 'rest/sttwebservice/search/'+formData["dsname"]+'/box'
            +'/'+formData["swLat"]+','+formData["swLng"] + '/'+formData["neLat"]+','+formData["neLng"]
            +'/'+sDate.getTime()+'/'+ eDate.getTime() + '/' + getLatLonRes() + '/count/null/emage';

            //var query = 'webresources/queryservice/asterixds/'+formData["dsname"]+'/'
            //    + formData["selectedTheme"] +'/'+formData["swLat"]+','+formData["swLng"]
            //    + '/'+formData["neLat"]+','+formData["neLng"]
            //    + '/'+formData["startdt"]+'/'+formData["enddt"]  + '/' + getLatLonRes() + '/emage';
            $('#emage-query').html(query);
            startTimer = performance.now()
            $.ajax( {
                url: query,
                type: 'GET',
                success: function( response ) {
                    //console.log(response);
                    // select as Emage
                    queryCallback(response);
                }
            } );
        });
    }
    function setParameter(startdate, enddate){
        var dsname = $("#dsname").val();
        var startdp = $("#start-date").datepicker("getDate");
        var enddp = $("#end-date").datepicker("getDate");
        var startdt = $.datepicker.formatDate("yy-mm-dd", startdp)+"T00:00:00Z";
        if(startdate != "")
            startdt = startdate;
        var enddt = $.datepicker.formatDate("yy-mm-dd", enddp)+"T23:59:59Z";
        if(enddate != "")
            enddt = enddate;
        var formData = {
            "dsname": dsname,
            "startdt": startdt,
            "enddt": enddt,
            "gridlat": $("#grid-lat-slider").slider("value"),
            "gridlng": $("#grid-lng-slider").slider("value")
        };

        // Get Map Bounds
        var bounds;
        if ($('#selection-label').hasClass("active") && selectionRectangle) {
            bounds = selectionRectangle.getBounds();
        } else {
            bounds = map.getBounds();
        }
        console.log(bounds);

        var swLat = bounds.getSouthWest().lat();
        var swLng = bounds.getSouthWest().lng();
        var neLat = bounds.getNorthEast().lat();
        var neLng = bounds.getNorthEast().lng();

        formData["swLat"] = Math.min(swLat, neLat);
        formData["swLng"] = Math.min(swLng, neLng);
        formData["neLat"] = Math.max(swLat, neLat);
        formData["neLng"] = Math.max(swLng, neLng);
        console.log('after', swLat, ',' , swLng, ',' ,neLat,  ',', neLng);
        if(neLng < swLng){	// world is round!!!
            formData["swLng"] = "-180.0";
        }

        // Get theme
        // This following function doesn't work, when the multiple-selection list is empty in the beginning.
        // Don't know the reason.
        //var selectedValues = $('#theme-select').val();
        //alert(selectedValues)
        //$('#theme-select :selected').each(function(i, sel){
        //    alert( $(sel).val());
        //});
        var selectedTheme = []
        $('.sol-selected-display-item-text').each(function(i, obj) {
            //alert($(this).text());
            selectedTheme[i] = $(this).text();
        });
        if(selectedTheme.length == 0)
            formData["selectedTheme"] = "null";
        else
            formData["selectedTheme"] = selectedTheme;
        return formData;
    }
    /**
     * Creates a delete icon button using default trash icon
     * @param    {String}    id, id for this element
     * @param    {String}    attachTo, id string of an element to which I can attach this button.
     * @param    {Function}  onClick, a function to fire when this icon is clicked
     */
    function addDeleteButton(iconId, attachTo, onClick) {

        var trashIcon = '<button class="btn btn-default" id="' + iconId + '"><span class="glyphicon glyphicon-trash"></span></button>';
        $('#' + attachTo).append(trashIcon);

        // When this trash button is clicked, the function is called.
        $('#' + iconId).on('click', onClick);
    }

    /**
     * Creates a message and attaches it to data management area.
     * @param    {String}    message, a message to post
     * @param    {Boolean}   isPositiveMessage, whether or not this is a positive message.
     * @param    {String}    target, the target div to attach this message.
     */
    function reportUserMessage(message, isPositiveMessage, target) {
        // Clear out any existing messages
        $('#' + target).html('');

        // Select appropriate alert-type
        var alertType = "alert-success";
        if (!isPositiveMessage) {
            alertType = "alert-danger";
        }

        // Append the appropriate message
        $('<div/>')
            .attr("class", "alert " + alertType)
            .html('<button type="button" class="close" data-dismiss="alert">&times;</button>' + message)
            .appendTo('#' + target);
    }

    /**
     * mapWidgetResetMap
     *
     * [No Parameters]
     *
     * Clears ALL map elements - plotted items, overlays, then resets position
     */
    function mapWidgetResetMap() {

        mapWidgetClearMap();

        // Reset map center and zoom
        map.setCenter(new google.maps.LatLng(37, -10));	// USA
        //map.setCenter(new google.maps.LatLng(40.890002, -20.7000890));
        map.setZoom(2);

        // Selection button
        $("#selection-button").trigger("click");
        rectangleManager.setMap(map);
        rectangleManager.setDrawingMode(google.maps.drawing.OverlayType.RECTANGLE);

        // Remove E-mage output
        $('#emage-query').html('')
        $('#emage-output').html('')
        $('#emage-duration').html('')
    }

    /**
     * mapWidgetClearMap
     * Removes data/markers
     */
    function mapWidgetClearMap() {

        // Remove previously plotted data/markers
        for (c in map_cells) {
            map_cells[c].setMap(null);
            console.log('clear ' + c);
        }
        map_cells = [];

        $.each(map_info_windows, function(i) {
            map_info_windows[i].close();
        });
        map_info_windows = [];//{};

        for (m in map_tweet_markers) {
            map_tweet_markers[m].setMap(null);
        }
        map_tweet_markers = [];

        // Hide legend
        $("#rainbow-legend-container").hide();

        // Reenable submit button
        $("#submit-button").attr("disabled", false);

        // Hide selection rectangle
        if (selectionRectangle) {
            selectionRectangle.setMap(null);
            selectionRectangle = null;
        }
    }

    /**
     * buildLegend
     *
     * Generates gradient, button action for legend bar
     */
    function buildLegend() {

        // Fill in legend area with colors
        var gradientColor;

        for (i = 0; i<100; i++) {
            //$("#rainbow-legend-container").append("" + rainbow.colourAt(i));
            $("#legend-gradient").append('<div style="display:inline-block; max-width:2px; background-color:#' + rainbow.colourAt(i) +';">&nbsp;</div>');
        }
    }

    $('#dsname').on('change', function (e) {
        dsnameChange();
    });

    function dsnameChange(){
        var dataTheme = fthemelist;
        formData["dsname"] = $("#dsname").val();
        $("#visualization").html('');
        setTimeLine();

        // initialize sol
        $('#theme-select').searchableOptionList({
            data: fthemelist,
            maxHeight: '250px',
            showSelectionBelowList: true
        });
    }

});	// end javascript ready (all DOMs are loaded)




/**
 * Computes radius for a given data point from a spatial cell
 * @param    {Object}    keys => ["latSW" "lngSW" "latNE" "lngNE" "weight"]
 * @returns  {number}    radius between 2 points in metres
 */
/*
 function mapWidgetComputeCircleRadius(spatialCell, wLimit) {
 console.log(wLimit);
 // Define Boundary Points
 var point_center = new google.maps.LatLng((spatialCell.latSW + spatialCell.latNE)/2.0, (spatialCell.lngSW + spatialCell.lngNE)/2.0);
 var point_left = new google.maps.LatLng((spatialCell.latSW + spatialCell.latNE)/2.0, spatialCell.lngSW);
 var point_top = new google.maps.LatLng(spatialCell.latNE, (spatialCell.lngSW + spatialCell.lngNE)/2.0);

 // Circle scale modifier =
 var scale = 425 + 425*(spatialCell.weight / wLimit);
 var radius = scale * Math.min(distanceBetweenPoints(point_center, point_left), distanceBetweenPoints(point_center, point_top));
 console.log('radius:' + radius);

 // Return proportionate value so that circles mostly line up.
 return radius;
 }
 */
/**
 * Calculates the distance between two latlng locations in km, using Google Geometry API.
 * @param p1, a LatLng
 * @param p2, a LatLng
 */
/*
 function distanceBetweenPoints (p1, p2) {
 return 0.001 * google.maps.geometry.spherical.computeDistanceBetween(p1, p2);
 };
 */
