<?php
///////////////////////////////////////////////////////////////////////////////
// OBM - File : organizationalchart_index.php                                //
//     - Desc : Organizational Chart Index File                              //
// 2007-02-26 David PHAN                                                     //
///////////////////////////////////////////////////////////////////////////////
// $Id: organizationalchart_index.php,v 1.78 2007/02/19 14:32:51 mehdi Exp $ //
///////////////////////////////////////////////////////////////////////////////
// Actions :
// - index         -- search fields           -- show the user search form
// - search        -- search fields            -- show the result set of search
// - new           --                          -- show the new user form
// - detailconsult -- $organizationalchart_id  -- show the user detail
// - detailupdate  -- $organizationalchart_id  -- show the user detail form
// - insert        -- form fields              -- insert the user
// - reset         -- $organizationalchart_id  -- reset user preferences
// - update        -- form fields              -- update the user
// - check_delete  -- $organizationalchart_id  -- check links before delete
// - delete        -- $organizationalchart_id  -- delete the user
///////////////////////////////////////////////////////////////////////////////

$path = "..";
$module = "organizationalchart";
$obminclude = getenv("OBM_INCLUDE_VAR");
if ($obminclude == "") $obminclude = "obminclude";
include("$obminclude/global.inc");
$params = get_organizationalchart_params();
page_open(array("sess" => "OBM_Session", "auth" => $auth_class_name, "perm" => "OBM_Perm"));
include("$obminclude/global_pref.inc");

$extra_js_include[] = "organizationalchart.js";
$extra_css[] = $css_organizationalchart;

require("organizationalchart_display.inc");
require("organizationalchart_query.inc");
require("organizationalchart_js.inc");
include("$obminclude/of/of_category.inc");

get_organizationalchart_action();
$perm->check_permissions($module, $action);

update_last_visit("organizationalchart", $params["organizationalchart_id"], $action);

page_close();

///////////////////////////////////////////////////////////////////////////////
// Main Program                                                              //
///////////////////////////////////////////////////////////////////////////////

if ($action == "index" || $action == "") {
///////////////////////////////////////////////////////////////////////////////
  $display["search"] = dis_organizationalchart_search_form($params);
  if ($_SESSION['set_display'] == "yes") {
    $display["result"] = dis_organizationalchart_search_list($params);
  } else {
    $display["msg"] .= display_info_msg($l_no_display);
  }

} elseif ($action == "search") {
///////////////////////////////////////////////////////////////////////////////
  $display["search"] = dis_organizationalchart_search_form($params);
  $display["result"] = dis_organizationalchart_search_list($params);

} elseif ($action == "new") {
///////////////////////////////////////////////////////////////////////////////
  $display["detail"] = dis_organizationalchart_form($action, $params);

} elseif ($action == "insert") {
///////////////////////////////////////////////////////////////////////////////
  if(check_organizationalchart_data_form($params)) {
    $oid = run_query_organizationalchart_insert($params);
    if($oid > 0) {
      $params["organizationalchart_id"] = $oid;
      $display["detail"] = dis_organizationalchart_consult($params);
      $display["msg"] .= display_ok_msg("$l_organizationalchart : $l_insert_ok");
    } else {
	    $display["detail"] = dis_organizationalchart_form($action, $params);
      $display["msg"] .= display_err_msg("$l_organizationalchart : $l_insert_error");
    }
  // Form data are not valid
  } else {
    $display["msg"] = display_warn_msg($l_invalid_data . " : " . $err["msg"]);
    $display["detail"] = dis_organizationalchart_form($action, $params);
  }
} elseif ($action == "detailconsult") {
///////////////////////////////////////////////////////////////////////////////
  $display["detail"] = dis_organizationalchart_consult($params);

} else if ($action == "userdetail") {
///////////////////////////////////////////////////////////////////////////////
  $user_id = $params["user_id"];
  $usr_q = run_query_userobm($user_id);
  if ($usr_q->next_record()) {
    organizationalchart_json_event($usr_q);
    echo "({".$display['json']."})";
    exit();
  } else {
    exit();
  }
} elseif ($action == "detailupdate") {
///////////////////////////////////////////////////////////////////////////////
  $display["detail"] = dis_organizationalchart_form($action, $params);

} elseif ($action == "update") {
///////////////////////////////////////////////////////////////////////////////
  if (check_organizationalchart_data_form($params)) {
    $retour = run_query_organizationalchart_update($params["organizationalchart_id"], $params);
    if ($retour) {
      $display["msg"] .= display_ok_msg("$l_organizationalchart : $l_update_ok");
    } else {
      $display["msg"] .= display_err_msg("$l_organizationalchart : $l_update_error");
    }
    $display["detail"] = dis_organizationalchart_consult($params);
  } else {
    $display["msg"] .= display_warn_msg($l_invalid_data . " : " . $err["msg"]);
    $display["detail"] = dis_organizationalchart_form($action, $params);
  }

} elseif ($action == "export_ldif") {
///////////////////////////////////////////////////////////////////////////////
  organizationalchart_export_ldif($params);
  exit();

} elseif ($action == "check_delete") {
///////////////////////////////////////////////////////////////////////////////
  if (check_can_delete_organizationalchart($params["organizationalchart_id"])) {
    $display["msg"] .= display_info_msg($ok_msg, false);
    $display["detail"] = dis_can_delete_organizationalchart($params["organizationalchart_id"]);
  } else {
    $display["msg"] .= display_warn_msg($err["msg"], false);
    $display["msg"] .= display_warn_msg($l_cant_delete, false);
    $display["detail"] = dis_organizationalchart_consult($params);
  }

} elseif ($action == "delete") {
///////////////////////////////////////////////////////////////////////////////
  if (check_can_delete_organizationalchart($params["organizationalchart_id"])) {
    $retour = run_query_organizationalchart_delete($params["organizationalchart_id"]);
    if ($retour) {
      $display["msg"] .= display_ok_msg("$l_organizationalchart : $l_delete_ok");
    } else {
      $display["msg"] .= display_err_msg("$l_organizationalchart : $l_delete_error");
    }
    $display["search"] = dis_organizationalchart_search_form($params);
  } else {
    $display["msg"] .= display_warn_msg($err["msg"], false);
    $display["msg"] .= display_warn_msg($l_cant_delete, false);
    $display["detail"] = dis_organizationalchart_consult($params);
  }

} elseif ($action == "display") {
///////////////////////////////////////////////////////////////////////////////
  $prefs = get_display_pref($obm["uid"], "organizationalchart", 1);
  $display["detail"] = dis_organizationalchart_display_pref($prefs);

} else if ($action == "dispref_display") {
///////////////////////////////////////////////////////////////////////////////
  update_display_pref($entity, $fieldname, $fieldstatus);
  $prefs = get_display_pref($obm["uid"], "organizationalchart", 1);
  $display["detail"] = dis_organizationalchart_display_pref($prefs);

} else if ($action == "dispref_level") {
///////////////////////////////////////////////////////////////////////////////
  update_display_pref($entity, $fieldname, $fieldstatus, $fieldorder);
  $prefs = get_display_pref($obm["uid"], "organizationalchart", 1);
  $display["detail"] = dis_organizationalchart_display_pref($prefs);

}

///////////////////////////////////////////////////////////////////////////////
// Display
///////////////////////////////////////////////////////////////////////////////
if (!$params["ajax"]) {
  $display["head"] = display_head($l_organizationalchart);
  if (! $params["popup"]) {
    update_organizationalchart_action();
    $display["header"] = display_menu($module);
  }
  $display["end"] = display_end();
}
display_page($display);


///////////////////////////////////////////////////////////////////////////////
// Stores User parameters transmited in $params hash
// returns : $params hash with parameters set
///////////////////////////////////////////////////////////////////////////////
function get_organizationalchart_params() {

  // Get global params
  $params = get_global_params("Organizationalchart");

  return $params;
}


///////////////////////////////////////////////////////////////////////////////
// User Action 
///////////////////////////////////////////////////////////////////////////////
function get_organizationalchart_action() {
  global $params, $actions, $path;
  global $l_header_find, $l_header_new, $l_header_display, $l_header_consult;
  global $l_header_update, $l_header_delete, $l_export;
  global $cright_read, $cright_write, $cright_read_admin, $cright_write_admin;

  $id = $params["organizationalchart_id"];

  // Index
  $actions["organizationalchart"]["index"] = array (
  	'Name'     => $l_header_find,
  	'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=index",
  	'Right'    => $cright_read,
  	'Condition'=> array ('all') );
  
  // Search
  $actions["organizationalchart"]["search"] = array (
  	'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=search",
  	'Right'    => $cright_read,
  	'Condition'=> array ('None') );
  
  // New
  $actions["organizationalchart"]["new"] = array (
  	'Name'     => $l_header_new,
  	'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=new",
  	'Right'    => $cright_write_admin,
  	'Condition'=> array ('search','index','detailconsult','insert','update','delete','display', 'export_ldif') );
  
  // Insert
    $actions["organizationalchart"]["insert"] = array (
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=insert",
      'Right'    => $cright_write_admin,
      'Condition'=> array ('None') );
  
  // Detail Consult
    $actions["organizationalchart"]["detailconsult"]  = array (
      'Name'     => $l_header_consult,
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=detailconsult&amp;organizationalchart_id=$id",
      'Right'    => $cright_read,
      'Condition'=> array ('detailupdate', 'export_ldif') );

  // User Detail
    $actions["organizationalchart"]["userdetail"]  = array (
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=userdetail",
      'Right'    => $cright_read,
      'Condition'=> array ('None') );


  // Detail Update
  $actions["organizationalchart"]["detailupdate"] = array (
    'Name'     => $l_header_update,
    'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=detailupdate&amp;organizationalchart_id=$id",
    'Right'    => $cright_write_admin,
    'Condition'=> array ('detailconsult', 'update', 'export_ldif') );  

  // Update
  $actions["organizationalchart"]["update"] = array (
    'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=update",
    'Right'    => $cright_write_admin,
    'Condition'=> array ('None') );

  // Ldif export
    $actions["organizationalchart"]["export_ldif"]  = array (
      'Name'     => $l_export,
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=export_ldif&amp;organizationalchart_id=$id",
      'Right'    => $cright_read,
      'Condition'=> array ('detailconsult', 'insert', 'update') );

  // Check Delete
  $actions["organizationalchart"]["check_delete"] = array (
    'Name'     => $l_header_delete,
    'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=check_delete&amp;organizationalchart_id=$id",
    'Right'    => $cright_write_admin,
    'Condition'=> array ('detailconsult', 'detailupdate', 'update', 'export_ldif') );

  // Delete
  $actions["organizationalchart"]["delete"] = array (
    'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=delete",
    'Right'    => $cright_write_admin,
    'Condition'=> array ('None') );

  // Display
    $actions["organizationalchart"]["display"] = array (
      'Name'     => $l_header_display,
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=display",
      'Right'    => $cright_read,
      'Condition'=> array ('all') );
  
  // Display Preferences
    $actions["organizationalchart"]["dispref_display"] = array (
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=dispref_display",
      'Right'    => $cright_read,
      'Condition'=> array ('None') );
  
  // Display Level
    $actions["organizationalchart"]["dispref_level"]  = array (
      'Url'      => "$path/organizationalchart/organizationalchart_index.php?action=dispref_level",
      'Right'    => $cright_read,
      'Condition'=> array ('None') );
}


///////////////////////////////////////////////////////////////////////////////
// Organizational Chart Actions updates (after processing, before displaying menu)
///////////////////////////////////////////////////////////////////////////////
function update_organizationalchart_action() {
  global $params, $actions, $path;

  $id = $params["organizationalchart_id"];
  if ($id > 0) {
    // Detail Consult
    $actions["organizationalchart"]["detailconsult"]["Url"] = "$path/organizationalchart/organizationalchart_index.php?action=detailconsult&amp;organizationalchart_id=$id";
    $actions["organizationalchart"]["detailconsult"]['Condition'][] = 'insert';

    // Detail Update
    $actions["organizationalchart"]["detailupdate"]['Url'] = "$path/organizationalchart/organizationalchart_index.php?action=detailupdate&amp;organizationalchart_id=$id";
    $actions["organizationalchart"]["detailupdate"]['Condition'][] = 'insert';

    // Check Delete
    $actions["organizationalchart"]["check_delete"]['Url'] = "$path/organizationalchart/organizationalchart_index.php?action=check_delete&amp;organizationalchart_id=$id";
    $actions["organizationalchart"]["check_delete"]['Condition'][] = 'insert';
   }
}
?>
