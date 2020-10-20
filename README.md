# AndroidBLE

## How to use

### Step 1: Add the JitPack maven repository to the list of repositories:

```gradle
repositories {
	jcenter()
	maven { url "https://jitpack.io" }
}
```

### Step 2: Add the dependency information:

```gradle
dependencies {
	implementation 'com.github.trifork:AndroidBLE:{VERSION}' // for example, version can be 1.0.0
}
```

That´s it. You should now have `BLEManager` available by `import com.trifork.bluetoothle.BLEManager;`

## How to deploy new version

This library is distributed with using JitPack.io.
There is no build process or manual upload or anything like that.

### Step 1:

Go to the [releases page](https://github.com/trifork/AndroidBLE/releases).

### Step 2:

Select "Draft a new release"

### Step 3:

Set a tag version. For example "v1.0.0"
Select @target to be the `master`branch.
Provide a release title if it should not be the tag version provided.

### Step 4:

Press "Publish release"
This will create a release and a corresponding tag.

🎉 Hurray! It is now available to users to start using in their Android projects.