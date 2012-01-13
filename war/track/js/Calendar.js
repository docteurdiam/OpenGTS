// ----------------------------------------------------------------------------
// Copyright 2007-2011, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2008/07/08  Martin D. Flynn
//     -Moved from Calendar.java
//  2008/08/15  Martin D. Flynn
//     -Wrapped Calendar handling inside a Calendar object
//  2008/08/17  Martin D. Flynn
//     -Added support for collapsible calendar.
//     -From/To calendars now can coordinate date selection constraints.
//  2008/09/12  Martin D. Flynn
//     -Added support for decrementing time hour.
//  2008/09/19  Martin D. Flynn
//     -Check for invalid month/day when initially setting the calendar date.
//  2008/10/16  Martin D. Flynn
//     -Hour:Minute can now be entered as a text field (requires the setting of
//      property "calendar.useTimeTextField" in 'private.xml').
//  2009/01/01  Martin D. Flynn
//     -Option for setting first calendar day-of-week (calFirstDOW)
//  2009/11/10  Martin D. Flynn
//     -Added 'PopUp' type Calendar action.
// ----------------------------------------------------------------------------

var calImageBaseDir = ".";

// ----------------------------------------------------------------------------

var CLASS_CAL_DIV               = "calDiv";                     
var CLASS_CAL_TABLE             = "calTable";                   
var CLASS_CAL_EXPAND_BAR        = "calExpandBar";               
var CLASS_CAL_COLLAPSE_BAR      = "calCollapseBar";             
var CLASS_CAL_TITLE_DATE_CELL   = "calTitleDateCell";           
var CLASS_CAL_TITLE_TABLE       = "calTitleTable";              
var CLASS_CAL_TITLE_FIELD_EXP   = "calTitleFieldExpanded";      
var CLASS_CAL_DATE_TIME_EXP     = "calDateTimeFieldExpanded";   
var CLASS_CAL_DATE_FIELD        = "calDateField";               
var CLASS_CAL_TIME_EXP          = "calTimeFieldExpanded";       
var CLASS_CAL_TIME_EXP_COLON    = "calTimeFieldExpandedColon";  
var CLASS_CAL_TITLE_FIELD_COLL  = "calTitleFieldCollapsed";     
var CLASS_CAL_DATE_TIME_COLL    = "calDateTimeFieldCollapsed";  
var CLASS_CAL_TIME_COLL         = "calTimeFieldCollapsed";      
var CLASS_CAL_TIME_DISABLED     = "calTimeFieldDisabled";
var CLASS_CAL_MONTH_ADVANCE     = "calMonthAdvance";
var CLASS_CAL_MONTH_HEADER_TBL  = "calMonthHeaderTable";
var CLASS_CAL_MONTH_NAME        = "calMonthName";
var CLASS_CAL_MONTH_DAYS_CELL   = "calMonthDaysCell";
var CLASS_CAL_MONTH_DAYS_TBL    = "calMonthDaysTable";
var CLASS_CAL_MONTH_DOW         = "calMonthDOW";
var CLASS_CAL_PREV_MONTH_DAYS   = "calPrevMonthDays";
var CLASS_CAL_NEXT_MONTH_DAYS   = "calNextMonthDays";
var CLASS_CAL_MONTH_DAYS_ENABLE = "calThisMonthDaysEnabled";
var CLASS_CAL_MONTH_DAYS_DISABL = "calThisMonthDaysDisabled";

var DIVBOX_OVERRIDE_ONMOUSEDOWN = false;

// ----------------------------------------------------------------------------

/**
*** Sets the selected timezone
**/
function calSelectTimeZone(tmz) 
{
    calSelectedTMZ = tmz;
}

/**
*** Gets the selected timezone
**/
function calGetTimeZone()
{
    return calSelectedTMZ;
}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/* reset 'calDiv' dimensions */
function calResetDimensions(calFr, calTo)
{
    if ((calFr != null) && (calTo != null)) {

        /* reset width */
        var minW = Math.max(calFr.getWidth(), calTo.getWidth());
        calFr.setMinimumWidth(minW);
        calTo.setMinimumWidth(minW);

        /* reset height */
        var calDiv    = document.getElementById(ID_CAL_DIV);
        var calBottom = document.getElementById(ID_CAL_BOTTOM);
        if (calDiv && calBottom) {
            var calDivPos    = getElementPosition(calDiv);
            var calBottomPos = getElementPosition(calBottom);
            var minH = Math.abs(calBottomPos.top - calDivPos.top) + 3;
            calDiv.style.height = minH;
        } else {
            //alert("Unable to recalc 'calDiv' height");
        }

    }
}

/**
*** Bind From/To calendars and write their html into the page
*** (used only to initially draw calendars)
**/
function calWriteCalendars(calFr, calTo)
{
    if (calFr == calTo) { calTo = null; }
    if ((calFr != null) && (calTo != null)) {

        /* bind calendars */
        calFr.setNextCalendar(calTo);
        calTo.setPriorCalendar(calFr);
        
        /* draw calendars */
        var expandFr = !calFr.drawDivBox;
        var fade     = false;
        calFr.setExpanded(expandFr, fade);      // show expanded
        calTo.setExpanded(false   , fade);      // show collapsed

        /* reset dimensions (initial calendar draw) */
        calResetDimensions(calFr, calTo);

    } else
    if (calFr != null) {
        
        /* draw single "From" calendar */
        var expand = !calFr.drawDivBox;
        var fade   = false;
        calFr.setExpanded(expand, fade);
        
    } else
    if (calTo != null) {
        
        /* draw single "To" calendar */
        var expand = !calTo.drawDivBox;
        var fade   = false;
        calTo.setExpanded(expand, fade);
        
    }
}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

var CalMonthDays        = [ 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ];

var CAL_ID_TABLE        = ".table";
var CAL_ID_DATE         = ".ymd";
var CAL_ID_DAY          = ".day";
var CAL_ID_TIME         = ".time";
var CAL_ID_TIME_HH      = ".timeHH";
var CAL_ID_TIME_MM      = ".timeMM";

var COLOR_THISDAY_BG    = "#EFFFFF";
var COLOR_THISDAY_BGS   = "#00EEEE";

var ARROW_DOWN_CHAR     = "&#9660;";
var ARROW_UP_CHAR       = "&#9650;";

var FADE_INTERVAL_MS    = 1;

/**
*** Create Calendar object
*** @param calVarName  The name of the variable holding this calendar objects.  This is
***                    needed for self referencing when creating the calendar HTML.
*** @param calFormID   The form element that is to contain the selected date/time.  This 
***                    value is lazily evaluated to determine the form element.
*** @param year        The starting year
*** @param month1      The starting month
*** @param day         The starting day
*** @param eodHour     Should be either 0 or 23
**/
function Calendar(calVarName,calFormID,calTitle,year,month1,day,eodHour,eodMin)
{

    this.calVarName     = calVarName;
    this.calID          = calVarName;
    this.calFormID      = calFormID;

    this.title          = calTitle;
    this.enabled        = true;

    this.year           = parseInt(year);
    this.month1         = parseInt(month1);
    if (this.month1 <  1) { this.month1 =  1; }
    if (this.month1 > 12) { this.month1 = 12; }
    this.selDay         = parseInt(day);
    this.constrainSelectedDay(false);

    this.eodHour        = eodHour;
    this.selMin         = this.eodMin;

    this.eodMin         = eodMin;
    this.selHour        = this.eodHour;

    this.nextCal        = null;
    this.priorCal       = null;

    this.enableFade     = true;
    this.collapsible    = false;
    this.expanded       = false;
    
    this.divCalView     = null;
    this.divOnmousedown = null;
    
    this.drawDivBox     = false;
    this.drawClickBar   = false;
    this.drawFixedExpanded = true;
    
    this.didInit        = false; // true when calander has been drawn at least once

}

// ----------------------------------------------------------------------------

/**
*** Sets this calendar 'enabled' state
**/
Calendar.prototype.setEnabled = function(state)
{
    this.enabled = state;
    return this;
}

/**
*** Gets this calendar 'enabled' state
**/
Calendar.prototype.isEnabled = function()
{
    return this.enabled;
}

// ----------------------------------------------------------------------------

/**
*** Sets this calendar 'collapsible' state
**/
Calendar.prototype.setCollapsible = function(coll, fade, div)
{
    this.collapsible  = coll;
    this.enableFade   = fade;
    this.drawDivBox   = div;
    if (this.drawDivBox) { 
        this.collapsible = false; 
        this.enableFade  = false;
    }
    this.drawClickBar      = this.collapsible || this.drawDivBox;
    this.drawFixedExpanded = !this.collapsible && !this.drawDivBox;
    return this;
}

/**
*** Gets this calendar 'collapsible' state
**/
//Calendar.prototype.isCollapsible = function()
//{
//    return this.collapsible;
//}

// ----------------------------------------------------------------------------

/**
*** Gets the calendar table width
**/
Calendar.prototype.getWidth = function()
{
    var table = document.getElementById(this.calID + CAL_ID_TABLE);
    if (table != null) {
        var tableSize = getElementSize(table);
        return tableSize.width;
    } else {
        return 0;
    }
}

/**
*** Gets the calendar table width
**/
Calendar.prototype.setMinimumWidth = function(width)
{
    var div = document.getElementById(this.calID);
    if ((div != null) && (div.offsetWidth < width)) {
        div.style.width = width + 'px';
    }
}

// ----------------------------------------------------------------------------

/**
*** Sets the prior calendar whose dates should be _before_ this calendar
**/
Calendar.prototype.setPriorCalendar = function(cal)
{
    this.priorCal = cal;
}

/**
*** Returns true if this calendar is the same month as the prior calendar
**/
Calendar.prototype.isSameMonthAsPriorCalendar = function()
{
    if (this.priorCal == null) {
        return false;
    } else
    if (this.priorCal.getSelectedMonthNumber() >= this.getSelectedMonthNumber()) {
        return true;
    } else {
        return false;
    }
}

/**
*** Sets the next calendar whose dates should be _after_ this calendar
**/
Calendar.prototype.setNextCalendar = function(cal)
{
    this.nextCal = cal;
}

/**
*** Returns true if this calendar is the same month as the next calendar
**/
Calendar.prototype.isSameMonthAsNextCalendar = function()
{
    if (this.nextCal == null) {
        return false;
    } else
    if (this.nextCal.getSelectedMonthNumber() <= this.getSelectedMonthNumber()) {
        return true;
    } else {
        return false;
    }
}

/* return the other bound calendar */
Calendar.prototype.getOtherCalendar = function()
{
    return (this.nextCal != null)? this.nextCal : this.priorCal;
}

// ----------------------------------------------------------------------------

/**
*** Returns the number of days in the specified year/month
**/
Calendar.prototype.daysInMonth = function(year, month1)
{
    var isLeapYear = ((year % 4) == 0); // valid 1901 through 2999
    var m0 = ((month1 - 1) + 12) % 12;
    var daysInMonth = CalMonthDays[m0];
    if ((m0 == 1) && isLeapYear) { daysInMonth++; }
    return daysInMonth;
}

/**
*** Returns the day number of the specified date
**/
Calendar.prototype.dayNumber = function(year, month1, day)
{
    var yr = (year * 1000) + parseInt(((month1 - 3) * 1000) / 12);
    return parseInt((367 * yr + 625) / 1000) - (2 * parseInt(yr / 1000))
           + parseInt(yr / 4000) - parseInt(yr / 100000) + parseInt(yr / 400000)
           + day - 578042; // October 15, 1582, beginning of Gregorian Calendar
}

/**
*** Returns the day of week for the specified date
**/
Calendar.prototype.dayOfWeek = function(year, month1, day)
{
    return (this.dayNumber(year,month1,day) + 5) % 7;
}

/**
*** Converts the epoch timestamp to YYYY/MM/DD/hh/mm/ss
**/
Calendar.prototype.epochSecondsToYmdHms = function(epoch)
{
    var TOD      = epoch % (24 * 60 * 60); // in seconds
    var hour     = TOD / (60 * 60);
    var minute   = (TOD % (60 * 60)) / 60;
    var second   = (TOD % (60 * 60)) % 60;
    var N        = (epoch / (24 * 60 * 60)) + 719469;
    var C        = ((N * 1000) - 200) / 36524250;
    var N1       = N + C - (C / 4);
    var Y1       = ((N1 * 1000) - 200) / 365250;
    var N2       = N1 - ((365250 * Y1) / 1000);
    var M1       = ((N2 * 1000) - 500) / 30600;
    var day      = ((N2 * 1000) - (30600 * M1) + 500) / 1000;
    var month1   = (M1 <= 9)? (M1 + 3) : (M1 - 9);
    var year     = (M1 <= 9)? Y1 : (Y1 + 1);
    return { YYYY:year, MM:month1, DD:day, hh:hour, mm:minute, ss:second };
}

// ----------------------------------------------------------------------------

/**
*** Returns the YMD value as an argument for sending to the server
**/
Calendar.prototype.getArgDate = function()
{
    var YY = this.year;
    var MM = (this.month1  <= 9)? ("0" + this.month1 ) : this.month1;
    var DD = (this.selDay  <= 9)? ("0" + this.selDay ) : this.selDay;
    return YY + '/' + MM + '/' + DD; 
}

/**
*** Returns the YMDhm value as an argument for sending to the server
**/
Calendar.prototype.getArgDateTime = function()
{
    var hh = (this.selHour <= 9)? ('0' + this.selHour) : this.selHour;
    var mm = (this.selMin  <= 9)? ("0" + this.selMin)  : this.selMin;
    return this.getArgDate() + "/" + hh + ":" + mm; 
}

/**
*** Sets the form date/time
**/
Calendar.prototype.setFormDate = function()
{
    var YY = this.year;
    var MM = (this.month1  <= 9)? ("0" + this.month1 ) : this.month1;
    var DD = (this.selDay  <= 9)? ("0" + this.selDay ) : this.selDay;
    var hh = (this.selHour <= 9)? ('0' + this.selHour) : this.selHour;
    var mm = (this.selMin  <= 9)? ("0" + this.selMin ) : this.selMin;

    /* date */
    var dfld = document.getElementById(this.calID + CAL_ID_DATE);
    if (dfld != null) { 
        dfld.innerHTML = calFormatDisplayDate(YY,MM,DD) ; 
    }

    /* time */
    var tfld = document.getElementById(this.calID + CAL_ID_TIME);
    if (tfld != null) {
        tfld.innerHTML = hh + ":" + mm; 
    } else {
        var hhfld = document.getElementById(this.calID + CAL_ID_TIME_HH);
        if (hhfld != null) { 
            if (calTextTimeEntry) {
                hhfld.value = hh; 
            } else {
                hhfld.innerHTML = hh; 
            }
        }
        var mmfld = document.getElementById(this.calID + CAL_ID_TIME_MM);
        if (mmfld != null) { 
            if (calTextTimeEntry) {
                mmfld.value = mm; 
            } else {
                mmfld.innerHTML = mm; 
            }
        }
    }

    /* form */
    if (this.calFormID != null) {
        var formElem = eval("document."+this.calFormID);
        formElem.value = YY + '/' + MM + '/' + DD + "/" + hh + ":" + mm; 
    }

}

// ----------------------------------------------------------------------------

/**
*** returns the current selected day number
**/
Calendar.prototype.getSelectedDayNumber = function()
{
    return this.dayNumber(this.year, this.month1, this.selDay);
}

/**
*** returns the current selected month number
**/
Calendar.prototype.getSelectedMonthNumber = function()
{
    // this just needs to be an ascending value which uniquely identifies a given month
    return (this.year * 100) + this.month1;
}

/**
*** Constrain the selected day to the days within the current month
**/
Calendar.prototype.constrainSelectedDay = function(adjustMonth)
{
    if (this.selDay < 1) {
        if (adjustMonth) {
            // go to last day of previous month
            this.month1--;
            if (this.month1 < 1) {
                this.year--;
                this.month = 12;
            }
            this.selDay = this.daysInMonth(this.year, this.month1);
        } else {
            this.selDay = 1;
        }
    } else {
        var daysInMonth = this.daysInMonth(this.year, this.month1);
        if (this.selDay > daysInMonth) {
            if (adjustMonth) {
                // go to first day of next month
                this.month1++;
                if (this.month1 > 12) {
                    this.year++;
                    this.month = 1;
                }
                this.selDay = 1;
            } else {
                this.selDay = daysInMonth;
            }
        }
    }
    return this.selDay;
}

/**
*** Selects the specified calendar day
*** @param dy  The day to select
**/
Calendar.prototype.selectDay = function(dy,hr,mn)
{
    this.selDay = parseInt(dy);
    this.constrainSelectedDay(false);
    this.selHour = (typeof hr == "undefined")? this.eodHour : hr;
    this.selMin  = (typeof mn == "undefined")? this.eodMin  : mn;
    for (var d = 1; d <= 31; d++) {
        var md = document.getElementById(this.calID + CAL_ID_DAY + '.' + d);
        if (md != null) {
            var bg = (d==dy)? COLOR_THISDAY_BGS : COLOR_THISDAY_BG;
            md.style.backgroundColor = bg;
        }
    }
    this.setFormDate();
}

/**
*** Advance the time by specified number of hours
**/
Calendar.prototype.advanceHour = function(num)
{
    if (this.enabled) {
        if (num == 10) {
            if (this.selHour >= 14) {
                this.selHour = (this.selHour % 10);
            } else {
                this.selHour += 10;
            }
        } else
        if (num == -10) {
            if (this.selHour <=  3) { // 0,1,2,3
                this.selHour += 20;
            } else
            if (this.selHour <=  9) {
                this.selHour += 10;
            } else {
                this.selHour -= 10;
            }
        } else {
            this.selHour = (this.selHour + num + 24) % 24;
        }
        this.setFormDate();
    }
}

/**
*** Advance the time by specified number of minutes
**/
Calendar.prototype.advanceMinute = function(num)
{
    if (this.enabled) {
        this.selMin = (this.selMin + num + 60) % 60;
        this.setFormDate();
    }
}

/**
*** Advance the time by specified number of minutes
**/
Calendar.prototype.setInputHour = function()
{
    if (this.enabled) {
        var hhfld = document.getElementById(this.calID + CAL_ID_TIME_HH);
        if (hhfld != null) {
            var v = hhfld.value;
            this.selHour = numParseInt(v,this.eodHour);
            this.selHour = (this.selHour + 24) % 24;
        }
        this.setFormDate();
    }
}

/**
*** Advance the time by specified number of minutes
**/
Calendar.prototype.setInputMinute = function()
{
    if (this.enabled) {
        var mmfld = document.getElementById(this.calID + CAL_ID_TIME_MM);
        if (mmfld != null) {
            var v = mmfld.value;
            this.selMin = numParseInt(v,this.eodMin);
            this.selMin = (this.selMin + 60) % 60;
        }
        this.setFormDate();
    }
}

// ----------------------------------------------------------------------------

/*
Calendar.prototype.inputMinutes = function()
{
    if (this.minuteInputView != null) {
        this.closeTimeInputView();
    } else {
        var mmfld = document.getElementById(this.calID + CAL_ID_TIME_MM);
        if (mmfld != null) {
            var absLoc = getElementPosition(mmfld);
            var absSiz = getElementSize(mmfld);
            this.minuteInputView = createDivBox("calMinuteInputView", absLoc.left, absLoc.top + absSiz.height + 2, -1, -1);
            var html = "<select>";
            for (var m = 0; m < 60; m++) {
                var s = (m > 9)? m : ("0" + m);
                html += "<option value=' "+ m + "'>" + s + "</option>";
            }
            html += "</select>";
            this.minuteInputView.innerHTML = html;
            document.body.appendChild(this.minuteInputView);
        }
    }
}
Calendar.prototype.closeTimeInputView = function()
{
    if (this.minuteInputView != null) {
        document.body.removeChild(this.minuteInputView);
        this.minuteInputView = null;
    }
}
*/

// ----------------------------------------------------------------------------

/**
*** Toggle expand/collapse
**/
Calendar.prototype.toggleExpand = function()
{
    var fade = this.enableFade;
    this.setExpanded(!this.expanded, fade);
}

/**
*** Expand calendar
**/
Calendar.prototype.setExpanded = function(expand, fade)
{
    if (this.drawFixedExpanded) { expand = true; }
    if ((this.expanded != expand) || !this.didInit) {
        var otherCal = this.getOtherCalendar();
        if (this.drawDivBox && expand && otherCal && otherCal.expanded) {
            otherCal.setExpanded(false,false);
        }
        if (fade) {
            this.fadeLines = 0;
            this._fadeCalendar(expand);
        } else {
            this.expanded = expand;
            this.writeCalendar();
            calResetDimensions(this, otherCal);
        }
    }
}

/**
*** Fade calendar (in/out)
**/
Calendar.prototype._fadeCalendar = function(expand)
{
    var otherCal = this.getOtherCalendar();
    if (this.enableFade && (this.fadeLines <= 4)) {
        var thisRows = expand? this.fadeLines : (4 - this.fadeLines);
        var othrRows = 4 - thisRows;
        this._writeCalendar(true, thisRows);
        if (otherCal) { 
            otherCal._writeCalendar(true, othrRows); 
        }
        this.fadeLines++;
        setTimeout(this.calVarName+"._fadeCalendar("+expand+")",FADE_INTERVAL_MS);
    } else {
        this.expanded = expand;
        this.writeCalendar();
        if (otherCal) {
            otherCal.setExpanded(!this.expanded, false/*fade*/); 
        }
    }
}

// ----------------------------------------------------------------------------

/**
*** Sets the Year/Month/Day for this calendar
**/
Calendar.prototype.setDate = function(YYYY,MM,DD)
{
    this.year   = parseInt(YYYY);
    this.month1 = parseInt(MM);
    if (this.month1 <  1) { this.month1 =  1; }
    if (this.month1 > 12) { this.month1 = 12; }
    this.selDay = parseInt(DD);
    this.constrainSelectedDay(true); // advance month if day is beyond daysInMonth
    this.selHour = this.eodHour;
    this.selMin  = this.eodMin;
    this.setFormDate();
    this.writeCalendar();
}

/**
*** Rewrites the calendar for the previous month
**/
Calendar.prototype.gotoPreviousMonth = function()
{
    var wasSameAsNext = this.isSameMonthAsNextCalendar();
    this.month1 = this.month1 - 1;
    if (this.month1 <= 0) {
        this.month1 = 12;
        this.year = this.year - 1;
    }
    this.constrainSelectedDay(false);
    this.selHour = this.eodHour;
    this.selMin  = this.eodMin;
    this.setFormDate();
    this.writeCalendar();
    if (wasSameAsNext) {
        this.nextCal.writeCalendar();
    }
    if (this.isSameMonthAsPriorCalendar()) {
        this.priorCal.writeCalendar();
    }
};

/**
*** Rewrites the calendar for the next month
**/
Calendar.prototype.gotoNextMonth = function()
{
    var wasSameAsPrior = this.isSameMonthAsPriorCalendar();
    this.month1 = this.month1 + 1;
    if (this.month1 > 12) {
        this.month1 = 1;
        this.year = this.year + 1;
    }
    this.constrainSelectedDay(false);
    this.selHour = this.eodHour;
    this.selMin  = this.eodMin;
    this.setFormDate();
    this.writeCalendar();
    if (wasSameAsPrior) {
        this.priorCal.writeCalendar();
    }
    if (this.isSameMonthAsNextCalendar()) {
        this.nextCal.writeCalendar();
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Write the calendar HTML
**/
Calendar.prototype.writeCalendar = function()
{
    this._writeCalendar(this.expanded, 6);
    this.didInit = true;
}

/**
*** Write the calendar HTML
**/
Calendar.prototype._writeCalendar = function(expanded, calRows)
{
    //if (isNaN(this.year)  ) { alert("Invalid 'this.year'"); }
    //if (isNaN(this.month1)) { alert("Invalid 'this.month1'"); }

    var isLeapYear          = ((this.year % 4) == 0); // valid 1901 through 2399
    var daysInThisMonth     = this.daysInMonth(this.year, this.month1);
    var daysInLastMonth     = this.daysInMonth(this.year, this.month1 - 1);
    var monthName           = CalMonthNames[this.month1 - 1];
    var yearName            = (""+this.year).substring(2);

    var firstDayOfWeek      = this.dayOfWeek(this.year, this.month1, 1);

    var tableID             = this.calID + CAL_ID_TABLE;
    var dateID              = this.calID + CAL_ID_DATE;
    var timeID              = this.calID + CAL_ID_TIME;
    var hourID              = this.calID + CAL_ID_TIME_HH;
    var minuteID            = this.calID + CAL_ID_TIME_MM;
    var dayID               = this.calID + CAL_ID_DAY;

    var inTransition        = (calRows != 6);
    
    var calElem             = document.getElementById(this.calID);

    /* start calendar HTML */
    var calendarHTML = "";
    calendarHTML += "<table id='"+tableID+"' class='"+CLASS_CAL_TABLE+"' cellspacing='0' cellpadding='0'>\n";

    /* collapse/expand bar */
    var clickExpand = "";
    var styleCollapse = "";
    if (this.drawClickBar) {
        // display expand/collapse bar
        styleCollapse = "cursor:pointer;";
        var hasPrior  = (this.priorCal != null);
        calendarHTML += "<tr>";
        if (inTransition) {
            calendarHTML += "<td class='"+CLASS_CAL_EXPAND_BAR+"'>";
            // the borders don't display if there is nothing in this cell, however, we can't display any character
            // (even a space) as this can effect the height of the cell during the transition.
            //calendarHTML += "";
        } else {
            if (expanded) {
                clickExpand   = "javascript:"+this.calVarName+".toggleExpand();";
                calendarHTML += "<td class='"+CLASS_CAL_EXPAND_BAR+"' onclick=\""+clickExpand+"\" title=\""+TOOLTIP_COLLAPSE+"\">";
            } else {
                clickExpand   = "javascript:"+this.calVarName+".toggleExpand();";
                calendarHTML += "<td class='"+CLASS_CAL_COLLAPSE_BAR+"' onclick=\""+clickExpand+"\" title=\""+TOOLTIP_EXPAND+"\">";
            }
            var dirImage = null
            if (this.drawDivBox) {
                dirImage = expanded? "/images/arrowUp3.png" : "/images/arrowDn3.png";
            } else {
                dirImage = (hasPrior != expanded)? "/images/arrowUp3.png" : "/images/arrowDn3.png";
            }
            calendarHTML += "<img src=\"" + calImageBaseDir + dirImage + "\" height='5'>";
        }
        calendarHTML += "</td>";
        calendarHTML += "</tr>\n";
    }

    /* calendar title (ie "From   2008/08/08 | 00:00") */
    calendarHTML += "<tr><td class='"+CLASS_CAL_TITLE_DATE_CELL+"'>\n";
    calendarHTML += "<table class='"+CLASS_CAL_TITLE_TABLE+"' cellspacing='0' cellpadding='0'>\n";
    calendarHTML += "<tr>\n";
    if (expanded || this.drawFixedExpanded) {
        // display as expanded (or collapsible not allowed)
        var tooltip = this.drawClickBar && !inTransition? TOOLTIP_COLLAPSE : "";
        calendarHTML += "<td class='"+CLASS_CAL_TITLE_FIELD_EXP+"' style='"+styleCollapse+"' title=\""+tooltip+"\" onclick=\""+clickExpand+"\">"+this.title+"</td>\n";
        calendarHTML += "<td class='"+CLASS_CAL_DATE_TIME_EXP+"' style='"+styleCollapse+"' nowrap title=\""+tooltip+"\">";
        calendarHTML += "<span class='"+CLASS_CAL_DATE_FIELD+"' id='"+dateID+"' onclick=\""+clickExpand+"\"></span>&nbsp;";
        calendarHTML += "<span style='color:#999999'>|</span>&nbsp;";
        if (!this.enabled) {
            calendarHTML += "<span class='"+CLASS_CAL_TIME_DISABLED+"' id='"+timeID+"'></span>";
        } else
        if (inTransition) {
            calendarHTML += "<span class='"+CLASS_CAL_TIME_EXP+"' id='"+timeID+"'></span>";
        } else
        if (calTextTimeEntry) {
            var tw = 13; // pixel width: 12 for FF
            calendarHTML += "<input class='"+CLASS_CAL_TIME_EXP+"' id='"+hourID+"' title='"+TOOLTIP_SET_HOUR+"'" +
                "onkeypress=\"javascript:return "+this.calVarName+".timeDigitKey(event,0);\" " +
                "onchange=\"javascript:"+this.calVarName+".setInputHour();\" " + 
                "size='2' maxlength='2' style='border:0; margin:0; padding:0; width:"+tw+"px;'></input>";
            calendarHTML += "<span class='"+CLASS_CAL_TIME_EXP_COLON+"'>:</span>";
            calendarHTML += "<input class='"+CLASS_CAL_TIME_EXP+"' id='"+minuteID+"' title='"+TOOLTIP_SET_MINUTE+"'" +
                "onkeypress=\"javascript:return "+this.calVarName+".timeDigitKey(event,1);\" " +
                "onchange=\"javascript:"+this.calVarName+".setInputMinute();\" " + 
                "size='2' maxlength='2' style='border:0; margin:0; padding:0; width:"+tw+"px;'></input>";
        } else {
            calendarHTML += "<a class='"+CLASS_CAL_TIME_EXP+"' id='"+hourID+"' href=\"javascript:"+this.calVarName+".advanceHour(1);\" title='"+TOOLTIP_SET_HOUR+"'></a>";
            calendarHTML += "<span class='"+CLASS_CAL_TIME_EXP_COLON+"'>:</span>";
            calendarHTML += "<a class='"+CLASS_CAL_TIME_EXP+"' id='"+minuteID+"' href=\"javascript:"+this.calVarName+".advanceMinute(5);\" title='"+TOOLTIP_SET_MINUTE+"'></a>";
        }
        calendarHTML += "</td>\n";
    } else {
        // display as collapsed (and collapsible is allowed)
        var tooltip = !inTransition? TOOLTIP_EXPAND : "";
        calendarHTML += "<td class='"+CLASS_CAL_TITLE_FIELD_COLL+"' title=\""+tooltip+"\" onclick=\""+clickExpand+"\">"+this.title+"</td>\n";
        calendarHTML += "<td class='"+CLASS_CAL_DATE_TIME_COLL+"' title=\""+tooltip+"\">";
        calendarHTML += "<span class='"+CLASS_CAL_DATE_FIELD+"' id='"+dateID+"' onclick=\""+clickExpand+"\"></span>&nbsp;";
        calendarHTML += "<span style='color:#999999'>|</span>&nbsp;";
        if (!this.enabled) {
            calendarHTML += "<span class='"+CLASS_CAL_TIME_DISABLED+"' id='"+timeID+"'></span>";
        } else {
            calendarHTML += "<a class='"+CLASS_CAL_TIME_COLL+"' id='"+timeID+"' href=\""+clickExpand+"\"></a>";
        }
        calendarHTML += "</td>\n";
    }
    calendarHTML += "</td>\n";
    calendarHTML += "</tr>\n";
    calendarHTML += "</table>\n";
    calendarHTML += "</td></tr>\n";

    /* expanded? */
    var calendarBODY = "";
    if (expanded || this.drawFixedExpanded) {

        /* calendar month header */
        calendarBODY += "<tr><td>\n";
        calendarBODY += "<table class='"+CLASS_CAL_MONTH_HEADER_TBL+"' cellspacing='0' cellpadding='0'>\n";
        calendarBODY += "<tr>\n";
        if (!this.enabled) {
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_ADVANCE+"'>&nbsp;</td>\n";
        } else
        if (!this.isSameMonthAsPriorCalendar()) {
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_ADVANCE+"'><b><a href=\"javascript:"+this.calVarName+".gotoPreviousMonth()\" title=\""+TOOLTIP_PREV_MONTH+"\">&lt;&lt;&lt;&lt;</a></b></td>\n";
        } else {
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_ADVANCE+"'><b>&lt;&lt;&lt;&lt;</b></td>\n";
        }
        calendarBODY += "<td class='"+CLASS_CAL_MONTH_NAME+"' width='100%' nowrap><center><b>" + monthName + " '" + yearName + "</b></center></td>\n";
        if (!this.enabled) {
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_ADVANCE+"'>&nbsp;</td>\n"
        } else
        if (!this.isSameMonthAsNextCalendar()) {
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_ADVANCE+"'><b><a href=\"javascript:"+this.calVarName+".gotoNextMonth()\" title=\""+TOOLTIP_NEXT_MONTH+"\">&gt;&gt;&gt;&gt;</a></b></td>\n";
        } else {
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_ADVANCE+"'><b>&gt;&gt;&gt;&gt;</b></td>\n";
        }
        calendarBODY += "</tr>\n";
        calendarBODY += "</table>\n";
        calendarBODY += "</td></tr>\n";

        /* calendar days */
        calendarBODY += "<tr><td class='"+CLASS_CAL_MONTH_DAYS_CELL+"'>\n";
        calendarBODY += "<table class='"+CLASS_CAL_MONTH_DAYS_TBL+"' cellspacing='0' cellpadding='0'>\n";
        calendarBODY += "<tr>\n";
        for (var wdx = 0; wdx < 7; wdx++) {
            var wd = (wdx + calFirstDOW) % 7;
            calendarBODY += "<td class='"+CLASS_CAL_MONTH_DOW+"'><b>" + CalDayNames[wd] + "</b></td>\n";
        }
        calendarBODY += "</tr>\n";
        var d = -((firstDayOfWeek - calFirstDOW + 7) % 7);
        for (var i = 0; i < (calRows * 7);) { // 6 rows of 7 days
            calendarBODY += "<tr>\n";
            for (var wdx = 0; wdx < 7; wdx++) {
                var wd = (wdx + calFirstDOW) % 7;
                if (d < 0) {
                    // pre-month days
                    var ds = (d + daysInLastMonth + 1);
                    calendarBODY += "<td class='"+CLASS_CAL_PREV_MONTH_DAYS+"'><b>"+ds+"</b></td>\n";
                } else
                if (d >= daysInThisMonth) {
                    // next-month days
                    var ds = (d - daysInThisMonth + 1);
                    calendarBODY += "<td class='"+CLASS_CAL_NEXT_MONTH_DAYS+"'><b>"+ds+"</b></td>\n";
                } else {
                    // days in this month
                    var ds = (d + 1);
                    if (this.enabled/* && !inTransition*/) {
                        calendarBODY += "<td class='"+CLASS_CAL_MONTH_DAYS_ENABLE+"' id='"+dayID+"."+ds+"' onclick=\"javascript:"+this.calVarName+".selectDay("+ds+");\"><b>"+ds+"</b></td>\n";
                    } else {
                        calendarBODY += "<td class='"+CLASS_CAL_MONTH_DAYS_DISABL+"' id='"+dayID+"."+ds+"'><b>"+ds+"</b></td>\n";
                    }
                }
                d++;
                i++;
            }
            calendarBODY += "</tr>\n";
        }
        calendarBODY += "</table>\n";
        calendarBODY += "</td></tr>\n";

    }
    
    if (this.drawDivBox) {

        /* base HTML */
        calElem.innerHTML = calendarHTML + "</table>\n";
        // calResetDimensions

        /* calendar month body */
        this._divCloseCalendar();
        if (calendarBODY != "") {
            // create/display new DIV
            var absLoc = getElementPosition(calElem);
            var absSiz = getElementSize(calElem);
            this.divCalView = createDivBox(CLASS_CAL_DIV, absLoc.left + 1, absLoc.top + absSiz.height + 1, -1, -1);
            this.divCalView.innerHTML = 
                "<table class='"+CLASS_CAL_TABLE+"' cellspacing='0' cellpadding='0'>\n" +
                calendarBODY +
                "</table>\n";
            document.body.appendChild(this.divCalView);
            // set new 'onmousedown'
            /*
            if (DIVBOX_OVERRIDE_ONMOUSEDOWN) {
                this.divOnmousedown = document.onmousedown;
                var self = this;
                document.onmousedown = function(e) {
                    if (!e) var e = window.event;
                    if (!e) { return false; }
                    var targ = e.target? e.target : e.srcElement? e.srcElement : null;
                    if (targ && (targ.nodeType == 3)) { targ = targ.parentNode; } // Safari bug?
                    if (targ == calElem) {
                        return false;
                    } else {
                        for (;targ && (targ.nodeName != "BODY"); targ = targ.parentNode) {
                            if (targ == self.divCalView) { return false; }
                        }
                        self._divCloseCalendar();
                        return true;
                    }
                };
            }
            */
        }
        
    } else {
        
        calElem.innerHTML = calendarHTML + calendarBODY + "</table>\n";
        
    }
    
    /* reselect day */
    this.selectDay(this.selDay, this.selHour, this.selMin);

}

/* close the DIV calendar */
Calendar.prototype._divCloseCalendar = function()
{
    if (this.divCalView != null) {
        document.body.removeChild(this.divCalView);
        this.divCalView = null;
        if (this.divOnmousedown) {
            document.onmousedown = this.divOnmousedown;
            this.divOnmousedown  = null;
        }
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/* return true if this event represents an Digit key (0..9) */
Calendar.prototype.timeDigitKey = function(event, isMin)
{
    
    if (!this.enabled) {
        return true; // pass through
    }
    
    var code = getKeyCode(event);
    if ((code >= 0x30) && (code <= 0x39)) { // digit
        return true; // pass through
    } else
    if (code == 0x08) { // backspace
        return true; // pass through
    } else
    if (code == 0x09) { // tab
        return true; // pass through
    } else
    if ((code == 37) || (code == 39)) { // Left("%")/Right("'") arrows
        return true; // pass through
    } else
    if ((code == 38) || (code == 33) || (code == 117) || (code == 85)) { // UpArrow("&"), PageUp("!"), "u", "U"
        var inc = ((code == 33) || event.ctrlKey || event.shiftKey)? 10 : 1;
        if (isMin) { 
            this.setInputMinute();
            this.advanceMinute(inc); 
        } else { 
            this.setInputHour();
            this.advanceHour(inc); 
        }
        return false;
    } else
    if ((code == 40) || (code == 34) || (code == 100) || (code == 68)) { // DownArrow("("), PageDown('"'), "d", "D"
        var dec = ((code == 34) || event.ctrlKey || event.shiftKey)? -10 : -1;
        if (isMin) {
            this.setInputMinute();
            this.advanceMinute(dec); 
        } else { 
            this.setInputHour();
            this.advanceHour(dec); 
        }
        return false;
    } else
    if ((code == 36) || (code == 104) || (code == 72)) { // Home("$"), "h", "H"
        if (isMin) { 
            this.selMin = 0;
            this.setFormDate();
        } else { 
            this.selHour = 0;
            this.setFormDate();
        }
        return false;
    } else
    if ((code == 35) || (code == 101) || (code == 69)) { // End("#"), "e", "E"
        if (isMin) { 
            this.selMin = 59;
            this.setFormDate();
        } else { 
            this.selHour = 23;
            this.setFormDate();
        }
        return false;
    } else {
        //alert("code = " + code);
        return false; // ignore this char
    }
}

// ----------------------------------------------------------------------------
