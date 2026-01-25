# Store and Retrieve Garbage Day Dates

London, ON has a constantly shifting garbage schedule.  This project provides a back-end which allows
setting and getting dates that are garbage days.

By creating small Shortcuts on IOS, we can provide a UI to this backend.

Run the backend on a computer on your local network, which we'll call HOST.  The default port is 8081.

To set a garbage day, create a Shortcut called "Set Garbage Day" with three actions:
1. Ask for (Text) with (When is garbage day?)
2. Get contents of (HOST/saveGarbageDay), Method Post, Request Body (JSON), date (Ask for input)
3. Speak (Contents of URL).

To get garbage days, create a Shortcut called "Get Garbage Day" with two actions:
1. Get contents of (HOST/getGarbageDay)
2. Speak (Contents of URL).
