#pragma once

#include <array>

namespace Slic3r {
class Model;
void remap_model_filaments(Model& model, const std::array<int, 17>& source_to_target);
}
