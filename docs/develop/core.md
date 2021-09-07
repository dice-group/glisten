This section explains how to change and extend the Core library.




## Add my own Fact Generation Base List

Create a fact generation base list (or Statmenet Drawer) like the Allow list or Block list 


There are two steps involved.

1. Create your Statement Drawer
2. Add the Statment Drawer to the Configuration


### Create the Statment Drawer


To create a Statement Drawer we simply need to extend the `StmtDrawer` class.

Let's create our Drawer 

```kotlin

class MyDrawer2000(private val blockList: Collection<String>, private val seed: Long, override val model : Model, private val minPropOcc: Int, private val maxPropertyLimit: Int) : StmtDrawer(seed, model, minPropOcc, maxPropertyLimit) {

		//This is the heart of our Drawer
	    override fun getStmts(): MutableList<Statement> {
	    	//create your statements here. and return them.
	    }	
}
```


Inside the `getStmts()` method we create and return the list of Statements to consider either for true or for false statement creation. 

If you want to completly rewrite how true and false statements are generated you need to change the `TaskDrawer` accordingly. 


### Add your Drawer to the Configuration

Now let's name our drawer `MyDrawer2000`

However we want to state our name inside the configuration just as we can state `Allowlist` or `Blocklist` as the type. 

To do this go to the `Configurations` file inside the package `org.dice_group.glisten.core.config.Configuration`.
There us the `Configuration` class and the method `createStmtDrawer` 

inside this method you'll find a when statment. 
Add your Drawer as following

```kotlin
    private fun createStmtDrawer(type: String, list: Collection<String>, seed: Long, model: Model, minPropOcc: Int, maxPropertyLimit: Int  ): StmtDrawer {
        //Add your new statement drawer type here to the `when` clause
        return when{
            type.lowercase(Locale.getDefault()) == "allowlist" ->
                AllowListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
            
            type == "MyDrawer2000" -> MyDrawer2000(list, seed, model, minPropOcc, maxPropertyLimit)


            else ->
                BlockListDrawer(list, seed, model, minPropOcc, maxPropertyLimit)
        }
    }
```

that is it. You can now use your Statement Drawer from the Configuration file. 
