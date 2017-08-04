clear
echo "VdpnDemo script started. Press Enter to navigate."
read
cd ../policy_demo
./create_policy.sh >> /dev/null &2>1
echo ""
echo "ADC rules added to FPC Config Datastore"
echo ""
echo "Check fpc topology?"
read
cd ..
./get-topology.sh 2>/dev/null | grep \{.*\} | python -m json.tool
echo ""
echo "Create DPNs?"
read
cd dpn-mirrorScripts
./createDpns.sh >> /dev/null &2>1
echo ""
echo "DPNs created!!"
echo ""
echo "Add DPNs to vDPN?"
read
./addDpnSimple.sh 1 >> /dev/null &2>1
sleep 2s
./addDpnSimple.sh 2 >> /dev/null &2>1
sleep 2s
cd ..
./get-topology.sh 2>/dev/null | grep \{.*\} | python -m json.tool
echo ""
echo "Create session?"
read
./bindclient.sh #>> /dev/null &2>1
./context_create.sh #>> /dev/null &2>1
./context_update.sh #>> /dev/null &2>1
echo ""
echo "Session created!!"
echo ""
echo "Delete session?"
read
./context_delete.sh #>> /dev/null &2>1
echo ""
echo "Session deleted!!"
read

echo ""
echo "Shutdown?"
cd dpn-mirrorScripts 
#./removeDpnSimple.sh 1
#./removeDpnSimple.sh 2
#read
./deleteDpns.sh #>> /dev/null &2>1

cd ../policy_demo
./delete_policy.sh #>> /dev/null &2>1
echo "VdpnDemo finished, thanks for playing!"
