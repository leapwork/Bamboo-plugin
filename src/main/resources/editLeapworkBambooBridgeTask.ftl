[@ww.textfield labelKey="leapwork.hostname.label" name="leapworkHostname" id="leapworkHostname" required=true style="max-width: 350px; "/]
[@ww.textfield labelKey="leapwork.port.label" name="leapworkPort" id="leapworkPort" required=true style="max-width: 80px; "/]
[@ww.checkbox labelKey="leapwork.https.label" name="leapworkHttps" toggle="true" descriptionKey="leapwork.https.description" /]
[@ww.textfield labelKey="leapwork.accessKey.label" name="leapworkAccessKey" id="leapworkAccessKey" required=true style="max-width: 350px; "/]
[@ww.textfield labelKey="leapwork.delay.label" name="leapworkDelay" id="leapworkDelay" required=false style="width: 80px;"/]
[@ww.select cssClass="builderSelectWidget" labelKey='leapwork.doneStatusAs.label' name='leapworkDoneStatusAs' list="statuses" id="leapworkDoneStatusAs" emptyOption=false style="width: 80px;"/]
[@ww.textfield labelKey="leapwork.report.label" name="leapworkReport" id="leapworkReport" required=true style="max-width: 350px;"/]
[@ww.textarea labelKey="leapwork.scheduleVariables.label" name="leapworkScheduleVariables" id="leapworkScheduleVariables" required=false style="max-width: 350px; height:80px;"/]
[@ww.checkbox labelKey="leapwork.autoReport.label" name="leapworkAutoReport" toggle=true descriptionKey="leapwork.autoReport.description" /]
[@ww.checkbox labelKey="leapwork.writePassedFlowKeyFrames.label" name="leapworkWritePassedFlowKeyFrames" toggle="false"/]
[@ww.textarea labelKey="leapwork.schNames.label" name="leapworkSchNames" id="schNames" readonly='false' required=true style="max-width: 350px;  height:80px;" emptyOption=false/]
[@ww.textfield labelKey="Warning: If you have identical schedule names, Please check schedules from the list or execution will result in Failure." name="leapworkSchWarning" id="schNamesWarning" readonly='true' required=false style="max-width: 1px;  height:1px;" /]
[@ww.textarea labelKey="leapwork.schIds.label" name="leapworkSchIds" id="schIds" required=false style="max-width: 350px;" readonly=true/]

<input type="button" class="aui-button aui-button-primary" id="mainButton" value="Select schedules" onclick="GetSch()" />
<div id="LeapworkContainer" class="popupDiv" style="position:static; width:334px;"></div>

<style type="text/css">

ul.ul-treefree { padding-left:25px; font-weight: bold;}
ul.ul-treefree ul { margin:0; padding-left:6px; }
ul.ul-treefree li { position:relative; list-style:none outside none; border-left:solid 1px #999; margin:0; padding:0 0 0 19px; line-height:23px; }
ul.ul-treefree li:before { content:""; display:block; border-bottom:solid 1px #999; position:absolute; width:18px; height:11px; left:0; top:0; }
ul.ul-treefree li:last-child { border-left:0 none; }
ul.ul-treefree li:last-child:before { border-left:solid 1px #999; }
ul.ul-dropfree div.drop { width:11px; height:11px; position:absolute; z-index:10; top:6px; left:-6px; background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABYAAAALCAIAAAD0nuopAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAE1JREFUeNpinDlzJgNlgAWI09LScEnPmjWLoAImrHpIAkwMFAMqGMGC6X44GzkIsHoQooAFTTVQKdbAwxOigyMsmIh3MC7ASHnqBAgwAD4CGeOiDhXRAAAAAElFTkSuQmCC"); background-position:-11px 0; background-repeat:no-repeat; cursor:pointer;}

 .popupDiv { background-color: white; background-clip:padding-box; overflow:hidden; position:absolute; display:none; padding:3px 0.5em; border: 1px solid; border-color: #ccc #666 #666 #ccc; border-color: rgba(0, 0, 0, 0.2); box-shadow: 0 5px 10px rgba(0, 0, 0, 0.2); text-align: left; z-index: 100;}
  html:not(.ua-mac) .popupDiv { -webkit-transform: translateZ(0); transform: none;}
 .popupDiv.quickLinksMenuPopup {width: auto; min-width: 25em;}
 .popupDiv.popupLoading {background-color: #FFF; padding: 5px !important;}
  #warningPopup {background-color: #ffc;}
  a.actionLinkNew {background-color: #fff; padding: 3px 10px 3px;}
  a.actionLinkNew:hover, a.actionLinkNewHover {background-color: #e3e9ef;}
  #changesPopup {width: 42em; padding: 0; z-index: 80;}
  #changesPopup .changesContainer { margin: 0; padding: 5px 10px;}
  #changesPopup .userChangesHeader { background-color: #F0F5FB; padding: 2px 5px; font-size: 95%; font-weight: bold; border: none;}
  #changesPopup .userChangesHeader.highlightChanges { background-color: #FFC;}
  #changesPopup .userChangeComment {overflow-x: hidden; margin-right: .5em; font-size: 90%; line-height: 1.5em;}
  #changesPopup .userChangeFiles { float: right; min-width: 6em; font-size: 85%; text-align: right;}
  #changesPopup .userChangeFiles span.highlightChanges{ left: 0;}
  #changesPopup .userChanges {border: none;}
  #changesPopup .userChange {clear: both; padding: 5px 0 5px 5px; border-bottom: 1px solid #E3E9EF;}
  #changesPopup .userChange:last-of-type {border: none;}
  #changesPopup .artifactCommentTable {width: 100%;}
  #changesPopup .artifactCommentBuildType {width: 30%; vertical-align: top;}
  #changesPopup .buildChangesHeader {font-weight: normal;}
  #changesPopup .buildStartDate {float: right;font-size: 90%;}
  #changesPopup .buildChanges {width: 100%;}
  #changesPopup .buildChanges td.username {width: 30%; font-weight: bold; font-size: 90%;}
  #changesPopup .buildChanges td.userChange {width: 70%;}
  #changesPopup .changesPopupTable {width: 100%; }
  #changesPopup .changesPopupTable th {padding-right: 1em;}
  #changesPopup .changesPopupTable td.files {text-align: right;}
  #changesPopup .changesPopupTable td.user { padding-right: 1.5em; text-align: right;}
  #changesPopup .changesPopupTable .date {padding-right: 1.5em;}
  #changesPopup .changesPopupTable td, #changesPopup .simpleTabs .tabs {margin: 5px 0 0 10px; overflow: auto; }
  #changesPopup table.buildChanges { border-collapse: collapse;}
  #changesPopup table.buildChanges td.username, #changesPopup table.buildChanges td.userChangeTD { border-bottom: 1px solid #E3E9EF;}
  #changesPopup table.buildChanges div.userChange {border: none;}
  #changesPopup .ellipsis, span.textExpandArrow { display: inline-block; width: 22px; height: 1em;background: #F3F3F3;border: 1px solid #C8C8C8;border-radius: 2px;color: #C8C8C8;text-align: center;}
  #changesPopup .ellipsis:hover, span.textExpandArrow:hover {cursor: pointer; background: #E3E3E3;}
  #changesPopup .historyBuildNote { margin-bottom: 4px;}
  #changesPopup .changePopupHeader, #changesPopup .changesPopupFooter {background-color: #f5f5f5;padding: 4px;font-size: 90%;}
  #changesPopup .changePopupHeader {margin-bottom: 6px; border-bottom: 1px solid #E3E9EF;}
  #changesPopup .changePopupFooter {margin-top: 6px;}
  #changesPopup .userChangeComment td.status, #changesPopup .userChangeComment td.buildNumber {vertical-align: top;}
  #changesPopup .userChangeComment td.buildNumber {width: 10%;}
  #changesPopup .artifactsChangeHeader {background-position: 5px 3px; padding-left: 25px;}
  span.changeName {font-size: 90%;}
  div.dependencyRelationIcon {float: right;}
  div.subrepoIcon {float: right;}

</style>

<script type="text/javascript">
    function checkBoxChangeValue(checkbox)
    {
       checkbox.value = checkbox.value === "false" ? "true" : "false";
       if(checkbox.value === "false")
        checkbox.removeAttribute("checked");
       else
        checkbox.setAttribute("checked","checked");
    }
</script>

<script type="text/javascript">

fieldArea_schIds.style.display='none';

         function GetSch() {
                   const leapworkHostname = document.getElementById("leapworkHostname").value;
                   const leapworkPort = document.getElementById("leapworkPort").value;
                   const leapworkConnectionType = document.getElementById("leapworkHttps").checked ? "https" : "http";

                   if(!leapworkHostname || !leapworkPort)
                   {
                     alert('"hostname or/and field is empty! Cannot connect to controller!"');
                   }
                   else
                   {
                       const address = leapworkConnectionType + "://" + leapworkHostname + ":" + leapworkPort;
                       const accessKey = document.getElementById("leapworkAccessKey").value;

                       if(document.getElementById('LeapworkContainer').innerHTML == "")
                       {

                           (jQuery).ajax({
                               url: address + "/api/v4/schedules",
                               headers: {'AccessKey': accessKey},
                               type: 'GET',
                               dataType:"json",
                               success: function(json)
                               {
                                     const container = document.getElementById("LeapworkContainer");

                                     (jQuery)(document).click(function (event) {
                                         if ((jQuery)(event.target).closest('#LeapworkContainer').length == 0 && (jQuery)(event.target).attr('id') != 'mainButton') {
                                             (jQuery)("#LeapworkContainer input:checkbox").remove();
                                             (jQuery)("#LeapworkContainer li").remove();
                                             (jQuery)("#LeapworkContainer ul").remove();
                                             (jQuery)("#LeapworkContainer br").remove();
                                             container.style.display = 'none';
                                         }
                                     });

                                     const schul = document.createElement('ul');
                                     schul.className = 'ul-treefree ul-dropfree';

                                     let schName = new Array();
                                     let schId = new Array();
                                     container.innerHTML += '<br>';

                                     for (let i = 0; i < json.length; i++) {
                                         schId.push(json[i].Id);
                                         schName.push(json[i].Title);

                                          let schli = document.createElement('li');
                                          let chbx = document.createElement('input');
                                          chbx.type = 'checkbox';
                                          chbx.name = schName[i];
                                          chbx.id = i;
                                          chbx.value = schId[i];

                                          if (json[i].IsEnabled != true)
                                          {
                                              chbx.disabled = true;
                                              schli.appendChild(chbx);
                                              schli.innerHTML+=schName[i].strike().italics().fontcolor("gray");
                                          }
                                          else
                                          {
                                              schli.appendChild(chbx);
                                              schli.innerHTML+=schName[i];
                                          }

                                          if(json[i].Type === "ScheduleInfo")
                                              schul.appendChild(schli);

                                     }
                                              container.appendChild(schul);
                                              container.innerHTML += '<br>';

                                              container.style.display='block';

                                              (jQuery)(".ul-dropfree").find("li:has(ul)").prepend('<div class="drop"></div>');
                                              	(jQuery)(".ul-dropfree div.drop").click(function() {
                                              		if ((jQuery)(this).nextAll("ul").css('display')=='none') {
                                              			(jQuery)(this).nextAll("ul").slideDown(400);
                                              			(jQuery)(this).css({'background-position':"-11px 0"});
                                              		} else {
                                              			(jQuery)(this).nextAll("ul").slideUp(400);
                                              			(jQuery)(this).css({'background-position':"0 0"});
                                              		}
                                              	});
                                              	(jQuery)(".ul-dropfree").find("ul").slideUp(400).parents("li").children("div.drop").css({'background-position':"0 0"});

                                              let TestNames = document.getElementById("schNames");
                                              let TestIds = document.getElementById("schIds");

                                              let boxes = (jQuery)("#LeapworkContainer input:checkbox");
                                              let existingTests = new Array();
                                              existingTests = TestNames.value.split(/\r\n|\n|\s+,\s+|,\s+|\s+,|,/);

                                              if (TestNames.value != null && TestIds.value != null) {
                                                     for (let i = 0; i < existingTests.length; i++) {
                                                         for (j = 0; j < boxes.length; j++)
                                                         {

                                                             if (existingTests[i] == boxes[j].getAttributeNode('name').value && !(jQuery)(boxes[j]).prop('checked'))
                                                              {

                                                                   if(boxes[j].disabled == false)
                                                                       (jQuery)(boxes[j]).prop('checked', 'checked');
                                                             			break;
                                                               }
                                                         }
                                                     }

                                              }

                                              (jQuery)("#LeapworkContainer input:checkbox").on("change", function ()
                                              {
                                                  let NamesArray = new Array();
                                                  let IdsArray = new Array();
                                                  for (let i = 0; i < boxes.length; i++)
                                                  {
                                                       let box = boxes[i];
                                                       if ((jQuery)(box).prop('checked'))
                                                       {
                                                             NamesArray[NamesArray.length] = (jQuery)(box).attr('name');
                                                             IdsArray[IdsArray.length] = (jQuery)(box).val();
                                                       }
                                                  }
                                                  TestNames.value = NamesArray.join("\n");
                                                  TestIds.value = IdsArray.join("\n");
                                                  console.log(TestIds.value)
                                              });

                               },
                               error: function(XMLHttpRequest, textStatus, errorThrown)
                               {
                                         alert(
                                         "Error occurred! Cannot get the list of Schedules!\n" +
                                         "Status: " + textStatus + "\n" +
                                         "Error: " + errorThrown + "\n" +
                                         "This may occur because of the next reasons:\n" +
                                         "1.Invalid controller hostname\n" +
                                         "2.Invalid port number\n" +
                                         "3.Invalid access key\n" +
                                         "4.Controller is not running or updating now, check it in services\n" +
                                         "5.Your Leapwork Controller API port is blocked.\nUse 'netstat -na | find \"9001\"' command, The result should be:\n 0.0.0.0:9001  0.0.0.0:0  LISTENING\n" +
                                         "6.Your browser has such a setting enabled that blocks any http requests from https\n" +
                                         "If nothing helps, please contact support https://leapwork.com/support"
                                         );
                               }
                           });
                       }
                       else
                       {
                           (jQuery)("#LeapworkContainer input:checkbox").remove();
                           (jQuery)("#LeapworkContainer li").remove();
                           (jQuery)("#LeapworkContainer ul").remove();
                           (jQuery)("#LeapworkContainer br").remove();
                           GetSch();
                       }

                   }
             }
</script>