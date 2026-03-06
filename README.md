# Store and Retrieve Garbage Day Dates

London, ON has a constantly shifting garbage schedule.  This project provides a back-end which allows
setting and getting dates that are garbage days.

By creating small Shortcuts on IOS, we can provide a UI to this backend.

# Setup

Right now, Eclipse is recommended to run it.  I may provide a Spring Boot version at some point.  Run the backend (ca.quines.garbageday.GarbageDayServer) in Eclipse on a computer on your local network, which we'll call HOST.  The default port is 8081.

By default, the server class will look for an interface that matches 192.168 or 10.0.  If you have interfaces that match both, or a different configuration, then provide a single argument.  The server will try to match an interface against the argument.

To set a garbage day, create an iOS Shortcut (or equivalent) called "Set Garbage Day" with three actions:
1. Ask for (Text) with (When is garbage day?)
2. Get contents of (HOST/saveGarbageDay), Method Post, Request Body (JSON), date (Ask for input)
3. Speak (Contents of URL).

To get garbage days, create a Shortcut called "Get Garbage Day" with two actions:
1. Get contents of (HOST/getGarbageDay)
2. Speak (Contents of URL).
