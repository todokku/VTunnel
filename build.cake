
//////////////////////////////////////////////////////////////////////
// ARGUMENTS
//////////////////////////////////////////////////////////////////////

var target = Argument("target", "Default");
var configuration = Argument("configuration", "Release");

//////////////////////////////////////////////////////////////////////
// PREPARATION
//////////////////////////////////////////////////////////////////////

// Define directories.
var buildDir = Directory("./") + Directory(configuration);
var netFrameworkVer = "net48";

var msBuildSettings = new MSBuildSettings()
    {
        Configuration = configuration,
        MaxCpuCount = System.Environment.ProcessorCount,
        Verbosity = Verbosity.Normal
    };

var dotnetSettings = new DotNetCorePublishSettings
    {
        Framework = netFrameworkVer,
        Configuration = "Release",
        OutputDirectory = buildDir + Directory("publish-" + netFrameworkVer)
    };

var cleanSettings = new DotNetCoreCleanSettings
    {
        Framework = dotnetSettings.Framework,
        Configuration = dotnetSettings.Configuration,
        OutputDirectory = dotnetSettings.OutputDirectory
    };

var licensefiles = new [] {
    "LICENSE"
};
//////////////////////////////////////////////////////////////////////
// TASKS
//////////////////////////////////////////////////////////////////////

Task("Clean")
    .Does(() =>
{
    CleanDirectories("./**/bin/" + configuration);
    CleanDirectories("./**/obj");
    CleanDirectory(buildDir);
});

Task("Restore-NuGet-Packages")
    .IsDependentOn("Clean")
    .Does(() =>
{
    NuGetRestore("./VTunnel.sln");
});

Task("Build")
    .IsDependentOn("Restore-NuGet-Packages")
    .Does(() =>
{
      // Use MSBuild
      MSBuild("./VTunnel.sln", msBuildSettings);

});

//Task("Run-Unit-Tests")
    //.IsDependentOn("Build")
    //.Does(() =>
//{
    //NUnit3("./src/**/bin/" + configuration + "/*.Tests.dll", new NUnit3Settings {
        //NoResults = true
        //});
//});



//////////////////////////////////////////////////////////////////////
// TASK TARGETS
//////////////////////////////////////////////////////////////////////

Task("Default")
    .IsDependentOn("Build");

//////////////////////////////////////////////////////////////////////
// EXECUTION
//////////////////////////////////////////////////////////////////////

RunTarget(target);
