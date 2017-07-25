clear
echo "DoEverything script started. Press Enter to navigate."
echo "Checking fpc topology..."
cd ..
./get-topology.sh 2>/dev/null | grep \{.*\} | python -m json.tool
echo ""
echo "Create DPNs?"
read
cd dpn-mirrorScripts
./createDpns.sh 1>/dev/null
./addDpnSimple.sh 1 1>/dev/null
./addDpnSimple.sh 2 1>/dev/null
cd ..
./get-topology.sh 2>/dev/null | grep \{.*\} | python -m json.tool
echo ""
echo "DPNs created!!"
echo ""
echo "Create session?"
read
./bindclient.sh 1>/dev/null
./context_create.sh 1>/dev/null
./context_update.sh 1>/dev/null
echo ""
echo "Session created!!"
echo ""
echo "Delete session?"
read
./context_delete.sh 1>/dev/null
echo ""
echo "Session deleted!!"
echo "DoEverything finished, thanks for playing!"
