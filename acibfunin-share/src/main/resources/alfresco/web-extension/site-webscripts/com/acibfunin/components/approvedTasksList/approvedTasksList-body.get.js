var widgets = [];

// Main component
widgets.push({
    id: "approvedTasksList",
    name: "Alfresco.component.approvedTasksList",
    options: {
        componentId: instance.object.id
    }
});

model.widgets = widgets;


function main(){

    var profileId = page.url.templateArgs["userid"];
    if (profileId != null){
        // load user details for the profile from the repo
        if (user.getUser(profileId) == null){
            // If the user does not exist then we are going to intentionally throw an error that will force Surf
            // to render the standard error page.
            throw new Error("A user attempted to access a profile page that no longer exists or is invalid.");
        }
    }

    var connector = remote.connect("alfresco");
    var result = connector.get("/tasks/files/approved");
    // Log the HTTP status and the first part of the body
    logger.log("approvedTasksList: /tasks/files/approved status=" + result.status);
    logger.log("approvedTasksList: body preview:\n" + String(result.response).substring(0, 500));

    model.year = String(new Date().getFullYear());
    model.username = user.name;

    model.entries = jsonUtils.toObject(result.response);

    // Skip "$activiti" id part
    if (model.entries && model.entries.entries && model.entries.entries.length > 0) {
        for (var i = 0; i < model.entries.entries.length; i++) {
            if (model.entries.entries[i].ID) {
                model.entries.entries[i].ID = model.entries.entries[i].ID.substring(9);
            }
        }
    } else {
        // No entries found, so we will return an empty array
        model.entries = [];
        model.entries.entries = [];
    }

    // Users belonging to CONTROL_CALIDAD have additional features in the page
    result = connector.get("/api/people/" + profileId + "?groups=true");
    user = jsonUtils.toObject(result.response);;

    model.member = "false";
    for (var i = 0; i < user.groups.length; i++) {
        if (user.groups[i].itemName == "GROUP_CONTROL_CALIDAD") {
            model.member = "true";
        }
    }

}

main();