var READYSTATE_COMPLETED = 4;
var HTTP_STATUS_OK = 200;
var POST = 'POST';
var CONTENT_TYPE = 'content-Type';
var TYPE_JSON = 'application/json';
var COMMAND = 'preview/commands/execute';
var OSC_COMMAND = 'osc/commands/execute';
var OSC_CMD_STAT = 'osc/commands/status';
var status;
var PREVIEW_FORMAT = 2;
var RESIZE_WIDTH = 0;
var RESIZE_QUALITY = 100;
var lastTpId = -1;
var triggerMacAddr = "";
function stopLivePreview() {
  var command = {
    name: 'camera.stopPreview',
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      console.log(this.responseText);
    } else {
      console.log('stop live preview failed');
    }
  };
  xmlHttpRequest.open(POST, COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}
function startLivePreview() {
  var _parameters = {
    formatNo: PREVIEW_FORMAT
  };
  var command = {
    name: 'camera.startPreview',
    parameters: _parameters
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      console.log(this.responseText);
    } else {
      console.log('start live preview failed');
    }
  };
  xmlHttpRequest.open(POST, COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}
function updatePreviwFrame(){
  var _parameters = {
    resizeWidth: RESIZE_WIDTH,
    quality: RESIZE_QUALITY
  };
  var command = {
    name: 'camera.getPreviewFrame',
    parameters: _parameters
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      var reader = new FileReader();
      reader.onloadend = function onLoad() {
        var img = document.getElementById('lvimg');
        img.src = reader.result;
      };
      reader.readAsDataURL(this.response);
      repeat();
    }
  };
  xmlHttpRequest.open(POST, COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.responseType = 'blob';
  xmlHttpRequest.send(JSON.stringify(command));
}
function repeat() {
  const d1 = new Date();
  while (true) {
    const d2 = new Date();
    if (d2 - d1 > 200) {
      break;
    }
  }
  updatePreviwFrame();
  updatePreviewStat();
  updateEv();
  updateSwitchBotList();
  updateChart();
}

function updatePreviewStat() {
  var command = {};
  command.name = 'camera.getPreviewStat';
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      var responseJson = JSON.parse(this.responseText);
      var elements = document.getElementsByName("preview")
      if (responseJson.results=='on') {
        elements[1].checked = true;
      } else {
        elements[0].checked = true;
      }
    } else {
      console.log('getPreviewStat failed');
    }
  };
  xmlHttpRequest.open(POST, COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}

function setEv(ev) {
  var _exposureCompensation = {
    exposureCompensation: ev
  };
  var _parameters = {
    options: _exposureCompensation
  };
  var command = {
    name: 'camera.setOptions',
    parameters: _parameters
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      console.log(this.responseText);
    } else {
      console.log('setOptions failed');
    }
  };
  xmlHttpRequest.open(POST, OSC_COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}
function updateEv() {
  var _optionNames = '[exposureCompensation]';
  var _parameters = {
    optionNames: _optionNames
  };
  var command = {
    name: 'camera.getOptions',
    parameters: _parameters
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      var responseJson = JSON.parse(this.responseText);
      var elements = document.getElementsByName("ev")
      if (responseJson.results.options.exposureCompensation==-2.0) {
        elements[0].checked = true;
      } else if (responseJson.results.options.exposureCompensation==-1.7) {
        elements[1].checked = true;
      } else if (responseJson.results.options.exposureCompensation==-1.3) {
        elements[2].checked = true;
      } else if (responseJson.results.options.exposureCompensation==-1.0) {
        elements[3].checked = true;
      } else if (responseJson.results.options.exposureCompensation==-0.7) {
        elements[4].checked = true;
      } else if (responseJson.results.options.exposureCompensation==-0.3) {
        elements[5].checked = true;
      } else if (responseJson.results.options.exposureCompensation==0.0) {
        elements[6].checked = true;
      } else if (responseJson.results.options.exposureCompensation==0.3) {
        elements[7].checked = true;
      } else if (responseJson.results.options.exposureCompensation==0.7) {
        elements[8].checked = true;
      } else if (responseJson.results.options.exposureCompensation==1.0) {
        elements[9].checked = true;
      } else if (responseJson.results.options.exposureCompensation==1.3) {
        elements[10].checked = true;
      } else if (responseJson.results.options.exposureCompensation==1.7) {
        elements[11].checked = true;
      } else if (responseJson.results.options.exposureCompensation==2.0) {
        elements[12].checked = true;
      }
    } else {
      console.log('getOptions failed');
    }
  };
  xmlHttpRequest.open(POST, OSC_COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}

function takePicture(){
  var command = {
    name: 'camera.takePicture'
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      console.log(this.responseText);
      var responseJson = JSON.parse(this.responseText);
      lastTpId = responseJson.id;
      setTimeout("watchTpComplete()",100);
      
    } else {
      console.log('setOptions failed');
    }
  };
  xmlHttpRequest.open(POST, OSC_COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}
function watchTpComplete(){
  var command = {
    id: lastTpId
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      console.log(this.responseText);
      var responseJson = JSON.parse(this.responseText);
      if (responseJson.results=='inProgress') {
        setTimeout("watchTpComplete()",100);
      } else {
        startLivePreview();
      }
    } else {
      console.log('setOptions failed');
    }
  };
  xmlHttpRequest.open(POST, OSC_CMD_STAT, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}

function updateChart() {
  
  if (triggerMacAddr!='') {
    var _parameters = {
      mac:triggerMacAddr
    };
    var command = {
      name: 'camera.getHumidityList',
      parameters: _parameters
    };
    var xmlHttpRequest = new XMLHttpRequest();
    xmlHttpRequest.onreadystatechange = function() {
      if (this.readyState === READYSTATE_COMPLETED &&
        this.status === HTTP_STATUS_OK) {
        
        var responseJson = JSON.parse(this.responseText);
        
        var tmpLabel = responseJson.results.time;
        var tmpData = responseJson.results.humidity;
        
        var data = {
          labels:tmpLabel,
          datasets:[
            {
              label:'humidity',
              data:tmpData,
              backgroundColor:'rgba(255, 255, 255, 0.0)',
              borderColor:'rgba(255, 0, 0, 1.0)',
            },
          ]
        }
        
        const ctx = document.getElementById('chart_humidity')
        const chart_cv = new Chart(ctx, {
          type: 'line',
          data: data,
          options: {
              animation: {
                  duration: 1,
              },
              hover: {
                  animationDuration: 0,
              },
              responsiveAnimationDuration: 0,
          }
        })
        
      } else {
        console.log('camera.getHumidityList failed');
      }
    };
    xmlHttpRequest.open(POST, COMMAND, true);
    xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
    xmlHttpRequest.send(JSON.stringify(command));
  }
}

function updateSwitchBotList() {
  var command = {};
  command.name = 'camera.getSwitchBotList';
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      
      var responseJson = JSON.parse(this.responseText);
      
      var title=["trigger", "MAC address ","Type(bot/TH) ","Bat[%] ","RSSI[db] ","Temperature[C] ","Humidity[%] "]
      
      var elmtTable = document.getElementById('scan_list');
      
      //Delete old table
      elmtTable.innerHTML="";
      
      //Create new table
      var rows=[];
      var table = document.createElement('table');
      table.border = 1;
      
      for(i = 0; i < responseJson.results.entries.length; i++){
        if (i==0) {
          rows.push(table.insertRow(-1));
          for(j = 0; j < title.length; j++){
            cell=rows[i].insertCell(-1);
            cell.appendChild(document.createTextNode(title[j]));
          }
        }
        
        rows.push(table.insertRow(-1));
        if ( responseJson.results.entries[i] != null ) {
          cell=rows[i+1].insertCell(-1);
          if ( responseJson.results.entries[i].type == 'TH' ) {
            cell.innerHTML = '<input type="button" value=set trigger" id="coladd" onclick="setTrigger(this)">'
          } else {
            cell.innerHTML = '---'
          }
          cell=rows[i+1].insertCell(-1);
          cell.appendChild(document.createTextNode(responseJson.results.entries[i].mac));
          if (responseJson.results.entries[i].trigger == 'true') {
            triggerMacAddr = responseJson.results.entries[i].mac;
            cell.style.backgroundColor = 'red';
          }
          cell=rows[i+1].insertCell(-1);
          cell.appendChild(document.createTextNode(responseJson.results.entries[i].type));
          cell=rows[i+1].insertCell(-1);
          cell.appendChild(document.createTextNode(responseJson.results.entries[i].bat));
          cell=rows[i+1].insertCell(-1);
          cell.appendChild(document.createTextNode(responseJson.results.entries[i].rssi));
          cell=rows[i+1].insertCell(-1);
          cell.appendChild(document.createTextNode(responseJson.results.entries[i].temperature));
          cell=rows[i+1].insertCell(-1);
          cell.appendChild(document.createTextNode(responseJson.results.entries[i].humidity));
          
          
        } else {
          for(j = 0; j < title.length; j++){
            cell=rows[i+1].insertCell(-1);
            cell.appendChild(document.createTextNode('null'));
          }
        }
        
      }
      elmtTable.appendChild(table);
      
    } else {
      console.log('getSwitchBotList failed');
    }
  };
  xmlHttpRequest.open(POST, COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}

function setTrigger(obj) {
  tr = obj.parentNode.parentNode;
  var setMac = tr.cells[1].innerText;
  var _parameters = {
    mac: setMac
  };
  var command = {
    name: 'camera.setTrigger',
    parameters: _parameters
  };
  var xmlHttpRequest = new XMLHttpRequest();
  xmlHttpRequest.onreadystatechange = function() {
    if (this.readyState === READYSTATE_COMPLETED &&
      this.status === HTTP_STATUS_OK) {
      console.log(this.responseText);
    } else {
      console.log('setTrigger failed');
    }
  };
  xmlHttpRequest.open(POST, COMMAND, true);
  xmlHttpRequest.setRequestHeader(CONTENT_TYPE, TYPE_JSON);
  xmlHttpRequest.send(JSON.stringify(command));
}