
in=$1

echo "测试数据${in}"

compile_output_path="./out/"
in_path="./in/${in}.in"
out_path="./in/${in}.out"

javac -encoding utf-8 -d ./out/  ./src/main/java/Main.java 

time java -cp ./out/ Main < $in_path >  $out_path